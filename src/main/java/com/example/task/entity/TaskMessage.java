package com.example.task.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RocketMQ message body for task processing
 * Lightweight message that only contains task ID and essential info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskMessage {

    private String taskId;

    private String taskType;

    private Integer retryCount;

    /**
     * Convert Task to TaskMessage
     */
    public static TaskMessage fromTask(Task task) {
        return TaskMessage.builder()
                .taskId(task.getId())
                .taskType(task.getTaskType())
                .retryCount(0)
                .build();
    }
}
