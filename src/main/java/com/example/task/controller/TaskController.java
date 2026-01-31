package com.example.task.controller;

import com.example.task.entity.Task;
import com.example.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Task Controller
 * Provides three endpoints:
 * 1. POST /tasks - Submit a task
 * 2. GET /tasks/{taskId} - Query task status
 * 3. GET /health - Health check for Docker
 */
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * Submit a task
     *
     * Request body example:
     * {
     *   "taskType": "DATA_EXPORT",
     *   "params": {
     *     "userId": "123",
     *     "dateRange": "2024-01-01:2024-12-31"
     *   }
     * }
     */
    @PostMapping("/tasks")
    public ResponseEntity<TaskResponse> submitTask(@RequestBody TaskRequest request) {
        log.info("Received task submission: taskType={}", request.getTaskType());

        // 1. Create task
        Task task = taskService.createTask(request.getTaskType(), request.getParams());

        // 2. Send to RocketMQ
        taskService.sendToQueue(task);

        // 3. Return task ID
        return ResponseEntity.ok(TaskResponse.builder()
                .taskId(task.getId())
                .status(task.getStatus().name())
                .createdAt(task.getCreatedAt())
                .build());
    }

    /**
     * Query task status by task ID
     */
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<TaskDetailResponse> getTask(@PathVariable String taskId) {
        log.info("Querying task: taskId={}", taskId);

        Task task = taskService.getTask(taskId);

        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(TaskDetailResponse.builder()
                .taskId(task.getId())
                .taskType(task.getTaskType())
                .status(task.getStatus().name())
                .progress(task.getProgress())
                .result(task.getResult())
                .errorMessage(task.getErrorMessage())
                .createdAt(task.getCreatedAt())
                .startedAt(task.getStartedAt())
                .finishedAt(task.getFinishedAt())
                .build());
    }

    /**
     * Health check endpoint for Docker
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    // Request DTO
    @lombok.Data
    @lombok.Builder
    public static class TaskRequest {
        private String taskType;
        private Map<String, Object> params;
    }

    // Response DTO for task submission
    @lombok.Data
    @lombok.Builder
    public static class TaskResponse {
        private String taskId;
        private String status;
        private java.time.LocalDateTime createdAt;
    }

    // Response DTO for task query
    @lombok.Data
    @lombok.Builder
    public static class TaskDetailResponse {
        private String taskId;
        private String taskType;
        private String status;
        private Integer progress;
        private Map<String, Object> result;
        private String errorMessage;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime startedAt;
        private java.time.LocalDateTime finishedAt;
    }
}
