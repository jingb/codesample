package com.example.task.consumer;

import com.example.task.entity.TaskMessage;
import com.example.task.service.TaskExecutionService;
import com.example.task.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Task Consumer using RocketMQ Official SDK
 * Demonstrates direct usage of RocketMQ client without Spring annotations
 */
@Slf4j
@Component
public class TaskConsumer {

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.consumer.group}")
    private String consumerGroup;

    @Value("${rocketmq.consumer.topic}")
    private String topic;

    @Value("${rocketmq.consumer.tag}")
    private String tag;

    @Value("${rocketmq.consumer.consume-thread-min:10}")
    private int consumeThreadMin;

    @Value("${rocketmq.consumer.consume-thread-max:10}")
    private int consumeThreadMax;

    @Value("${task.processing.max-retry-times:16}")
    private int maxRetryTimes;

    private final TaskService taskService;
    private final TaskExecutionService taskExecutionService;
    private final ObjectMapper objectMapper;

    private DefaultMQPushConsumer consumer;

    public TaskConsumer(TaskService taskService, TaskExecutionService taskExecutionService, ObjectMapper objectMapper) {
        this.taskService = taskService;
        this.taskExecutionService = taskExecutionService;
        this.objectMapper = objectMapper;
    }

    /**
     * Initialize and start RocketMQ consumer
     * Called automatically after Spring bean construction
     */
    @PostConstruct
    public void init() throws MQClientException {
        log.info("Initializing TaskConsumer...");

        // 1. Create consumer instance
        consumer = new DefaultMQPushConsumer(consumerGroup);

        // 2. Configure name server address
        consumer.setNamesrvAddr(nameServer);

        // 3. Configure thread pool (this controls the constant processing rate!)
        consumer.setConsumeThreadMin(consumeThreadMin);
        consumer.setConsumeThreadMax(consumeThreadMax);

        // 3.1. Configure max retry times
        consumer.setMaxReconsumeTimes(maxRetryTimes);
        log.info("Configured max retry times: {}", maxRetryTimes);

        // IMPORTANT: This is key to achieving constant throughput
        // With fixed thread pool size, the consumer can only process
        // a fixed number of tasks concurrently, ensuring resource usage
        // stays within expected bounds

        // 4. Subscribe to topic and tag
        consumer.subscribe(topic, tag);

        // 5. Register message listener
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(
                    List<MessageExt> messages,
                    ConsumeConcurrentlyContext context) {

                for (MessageExt message : messages) {
                    try {
                        // Process single message
                        ConsumeConcurrentlyStatus status = processMessage(message);

                        // If any message fails, return RECONSUME_LATER
                        if (status == ConsumeConcurrentlyStatus.RECONSUME_LATER) {
                            return status;
                        }

                    } catch (Exception e) {
                        log.error("Unexpected error processing message", e);
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }

                // All messages processed successfully
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        // 6. Start consumer
        consumer.start();

        log.info("TaskConsumer started successfully: group={}, topic={}, threads=[{}, {}], maxRetryTimes={}",
                consumerGroup, topic, consumeThreadMin, consumeThreadMax, maxRetryTimes);
    }

    /**
     * Process single message
     */
    private ConsumeConcurrentlyStatus processMessage(MessageExt message) {
        String taskId = "unknown";
        try {
            // 1. Parse message body
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            TaskMessage taskMessage = objectMapper.readValue(json, TaskMessage.class);
            taskId = taskMessage.getTaskId();

            int currentRetryCount = message.getReconsumeTimes();
            log.info("Processing message: taskId={}, reconsumeTimes={}, msgId={}",
                    taskId, currentRetryCount, message.getMsgId());

            // Sync retry count from RocketMQ
            if (currentRetryCount > 0) {
                taskService.incrementRetryCount(taskId);
            }

            // 2. Mark task as RUNNING
            taskService.markAsRunning(taskId);

            // 3. Get task details
            com.example.task.entity.Task task = taskService.getTask(taskId);
            if (task == null) {
                log.error("Task not found: taskId={}", taskId);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }

            // 4. Execute task (this may take time!)
            var result = taskExecutionService.execute(task);

            // 5. Mark task as SUCCESS
            taskService.markAsSuccess(taskId, result);

            log.info("Task processed successfully: taskId={}", taskId);

            // ACK: Tell RocketMQ this message is consumed successfully
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

        } catch (Exception e) {
            log.error("Failed to process task: taskId={}", taskId, e);

            // Mark task as FAILED
            taskService.markAsFailed(taskId, e.getMessage());

            // NACK: Tell RocketMQ to reconsume this message later
            // RocketMQ will retry based on delay level
            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
        }
    }

    /**
     * Shutdown consumer gracefully
     * Called when Spring context is destroyed
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down TaskConsumer...");

        if (consumer != null) {
            consumer.shutdown();
            log.info("TaskConsumer shut down successfully");
        }
    }
}
