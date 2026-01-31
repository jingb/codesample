package com.example.task.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_task_type", columnList = "task_type"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
public class Task {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "task_type", length = 64, nullable = false)
    private String taskType;

    /**
     * Task status: PENDING, RUNNING, SUCCESS, FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private TaskStatus status;

    /**
     * Task parameters (JSON string)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params", columnDefinition = "JSON")
    private Map<String, Object> params;

    /**
     * Task result (JSON string, only when status is SUCCESS)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result", columnDefinition = "JSON")
    private Map<String, Object> result;

    /**
     * Error message (only when status is FAILED)
     */
    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    /**
     * Progress: 0-100
     */
    @Column(name = "progress", nullable = false)
    private Integer progress;

    /**
     * Retry count
     */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public enum TaskStatus {
        PENDING,   // Task submitted, waiting to be processed
        RUNNING,   // Task is being processed
        SUCCESS,   // Task completed successfully
        FAILED     // Task failed
    }
}
