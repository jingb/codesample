package com.example.task.service;

import com.example.task.entity.Task;
import com.example.task.entity.TaskMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    // TODO: Replace with database repository
    // In-memory storage for demo purposes only
    private final Map<String, Task> taskStorage = new ConcurrentHashMap<>();

    /**
     * Create a new task
     */
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

        // TODO: Save to database to ensure task is not lost
        // INSERT INTO tasks (id, task_type, status, params, created_at)
        // VALUES (?, ?, ?, ?, ?)
        taskStorage.put(taskId, task);

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
            // TODO: Handle failure - update task status to FAILED
            updateTaskStatus(task.getId(), Task.TaskStatus.FAILED, e.getMessage());
        }
    }

    /**
     * Get task by ID
     */
    public Task getTask(String taskId) {
        // TODO: Query from database
        // SELECT * FROM tasks WHERE id = ?
        return taskStorage.get(taskId);
    }

    /**
     * Update task status to RUNNING
     */
    public void markAsRunning(String taskId) {
        Task task = taskStorage.get(taskId);
        if (task != null) {
            task.setStatus(Task.TaskStatus.RUNNING);
            task.setStartedAt(LocalDateTime.now());

            // TODO: Update database
            // UPDATE tasks SET status = 'RUNNING', started_at = ? WHERE id = ?

            log.info("Task marked as RUNNING: taskId={}", taskId);
        }
    }

    /**
     * Update task status to SUCCESS
     */
    public void markAsSuccess(String taskId, Map<String, Object> result) {
        Task task = taskStorage.get(taskId);
        if (task != null) {
            task.setStatus(Task.TaskStatus.SUCCESS);
            task.setResult(result);
            task.setProgress(100);
            task.setFinishedAt(LocalDateTime.now());

            // TODO: Update database
            // UPDATE tasks SET status = 'SUCCESS', result = ?, progress = 100, finished_at = ? WHERE id = ?

            log.info("Task marked as SUCCESS: taskId={}", taskId);
        }
    }

    /**
     * Update task status to FAILED
     */
    public void markAsFailed(String taskId, String errorMessage) {
        updateTaskStatus(taskId, Task.TaskStatus.FAILED, errorMessage);
    }

    /**
     * Update task progress
     */
    public void updateProgress(String taskId, int progress) {
        Task task = taskStorage.get(taskId);
        if (task != null) {
            task.setProgress(progress);

            // TODO: Update database
            // UPDATE tasks SET progress = ? WHERE id = ?

            log.debug("Task progress updated: taskId={}, progress={}", taskId, progress);
        }
    }

    /**
     * Update task status
     */
    private void updateTaskStatus(String taskId, Task.TaskStatus status, String errorMessage) {
        Task task = taskStorage.get(taskId);
        if (task != null) {
            task.setStatus(status);
            task.setErrorMessage(errorMessage);
            task.setFinishedAt(LocalDateTime.now());

            // TODO: Update database
            // UPDATE tasks SET status = ?, error_message = ?, finished_at = ? WHERE id = ?

            log.info("Task status updated: taskId={}, status={}", taskId, status);
        }
    }
}
