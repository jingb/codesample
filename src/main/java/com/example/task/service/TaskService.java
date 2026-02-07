package com.example.task.service;

import com.example.task.entity.Task;
import com.example.task.entity.TaskMessage;
import com.example.task.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Task Service
 * Handles task creation, persistence, and queue operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final DefaultMQProducer rocketMQProducer;
    private final ObjectMapper objectMapper;
    private final TaskRepository taskRepository;

    /**
     * Create a new task
     */
    @Transactional
    public Task createTask(String taskType, Map<String, Object> params) {
        String taskId = UUID.randomUUID().toString();

        Task task = Task.builder()
                .id(taskId)
                .taskType(taskType)
                .status(Task.TaskStatus.PENDING)
                .params(params)
                .progress(0)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        taskRepository.save(task);

        log.info("Task created: taskId={}, taskType={}", taskId, taskType);

        return task;
    }

    /**
     * Send task to RocketMQ
     */
    public void sendToQueue(Task task) {
        TaskMessage taskMessage = TaskMessage.fromTask(task);

        try {
            // 1. Convert message to JSON
            String messageJson = objectMapper.writeValueAsString(taskMessage);

            // 2. Create RocketMQ message
            Message message = new Message(
                    "task-topic",      // Topic
                    "*",                // Tag (all)
                    messageJson.getBytes(StandardCharsets.UTF_8)
            );

            // 3. Send message using official SDK
            rocketMQProducer.send(message);

            log.info("Task sent to queue: taskId={}", task.getId());
        } catch (Exception e) {
            log.error("Failed to send task to queue: taskId={}", task.getId(), e);
            updateTaskStatus(task.getId(), Task.TaskStatus.FAILED, e.getMessage());
        }
    }

    /**
     * Get task by ID
     */
    @Transactional(readOnly = true)
    public Task getTask(String taskId) {
        return taskRepository.findById(taskId).orElse(null);
    }

    /**
     * Update task status to RUNNING
     */
    @Transactional
    public void markAsRunning(String taskId) {
        taskRepository.findById(taskId).ifPresent(task -> {
            task.setStatus(Task.TaskStatus.RUNNING);
            task.setStartedAt(LocalDateTime.now());
            taskRepository.save(task);

            log.info("Task marked as RUNNING: taskId={}", taskId);
        });
    }

    /**
     * Update task status to SUCCESS
     */
    @Transactional
    public void markAsSuccess(String taskId, Map<String, Object> result) {
        taskRepository.findById(taskId).ifPresent(task -> {
            task.setStatus(Task.TaskStatus.SUCCESS);
            task.setResult(result);
            task.setProgress(100);
            task.setFinishedAt(LocalDateTime.now());
            taskRepository.save(task);

            log.info("Task marked as SUCCESS: taskId={}", taskId);
        });
    }

    /**
     * Update task status to FAILED
     */
    @Transactional
    public void markAsFailed(String taskId, String errorMessage) {
        updateTaskStatus(taskId, Task.TaskStatus.FAILED, errorMessage);
    }

    /**
     * Increment retry count
     */
    @Transactional
    public void incrementRetryCount(String taskId) {
        taskRepository.findById(taskId).ifPresent(task -> {
            int newCount = task.getRetryCount() + 1;
            task.setRetryCount(newCount);
            taskRepository.save(task);

            log.info("Task retry count incremented: taskId={}, retryCount={}", taskId, newCount);
        });
    }

    /**
     * Update task progress
     */
    @Transactional
    public void updateProgress(String taskId, int progress) {
        taskRepository.findById(taskId).ifPresent(task -> {
            task.setProgress(progress);
            taskRepository.save(task);

            log.debug("Task progress updated: taskId={}, progress={}", taskId, progress);
        });
    }

    /**
     * Update task status
     */
    @Transactional
    private void updateTaskStatus(String taskId, Task.TaskStatus status, String errorMessage) {
        taskRepository.findById(taskId).ifPresent(task -> {
            task.setStatus(status);
            task.setErrorMessage(errorMessage);
            task.setFinishedAt(LocalDateTime.now());
            taskRepository.save(task);

            log.info("Task status updated: taskId={}, status={}", taskId, status);
        });
    }
}
