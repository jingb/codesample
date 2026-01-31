package com.example.task.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Task entity
 * Represents an async task with its lifecycle states
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    private String id;

    private String taskType;

    /**
     * Task status: PENDING, RUNNING, SUCCESS, FAILED
     */
    private TaskStatus status;

    /**
     * Task parameters (JSON string)
     */
    private Map<String, Object> params;

    /**
     * Task result (JSON string, only when status is SUCCESS)
     */
    private Map<String, Object> result;

    /**
     * Error message (only when status is FAILED)
     */
    private String errorMessage;

    /**
     * Progress: 0-100
     */
    private Integer progress;

    /**
     * Retry count
     */
    private Integer retryCount;

    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    public enum TaskStatus {
        PENDING,   // Task submitted, waiting to be processed
        RUNNING,   // Task is being processed
        SUCCESS,   // Task completed successfully
        FAILED     // Task failed
    }
}
