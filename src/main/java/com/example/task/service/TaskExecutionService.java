package com.example.task.service;

import com.example.task.entity.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Task Executor
 * Executes actual task processing logic
 * This is where you implement your specific task handling
 */
@Slf4j
@Component
public class TaskExecutionService {

    private final Random random = new Random();

    @Value("${task.processing.duration-seconds:10}")
    private int taskDurationSeconds;

    // Track active tasks for monitoring concurrency
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    /**
     * Execute task based on task type
     */
    public Map<String, Object> execute(Task task) {
        int currentActive = activeTasks.incrementAndGet();
        log.info("Task started: taskId={}, taskType={}, activeTasks={}", task.getId(), task.getTaskType(), currentActive);

        try {
            // Simulate long-running task
            Map<String, Object> result = switch (task.getTaskType()) {
                case "DATA_EXPORT" -> handleDataExport(task);
                case "DATA_IMPORT" -> handleDataImport(task);
                case "REPORT_GENERATION" -> handleReportGeneration(task);
                default -> handleUnknownTask(task);
            };

            log.info("Task executed successfully: taskId={}, activeTasks={}", task.getId(), activeTasks.decrementAndGet());
            return result;

        } catch (Exception e) {
            activeTasks.decrementAndGet();
            log.error("Task execution failed: taskId={}", task.getId(), e);
            throw new RuntimeException("Task execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Handle data export task
     * Simulates a time-consuming export operation
     */
    private Map<String, Object> handleDataExport(Task task) {
        log.info("Handling data export: taskId={}, duration={}s", task.getId(), taskDurationSeconds);

        // Simulate progress updates over the configured duration
        int progressSteps = 10;
        long sleepPerStep = (taskDurationSeconds * 1000L) / progressSteps;

        for (int i = 1; i <= progressSteps; i++) {
            try {
                Thread.sleep(sleepPerStep); // Simulate work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Task interrupted", e);
            }

            int progress = i * 10;
            // TODO: Update progress to database or cache
            log.debug("Data export progress: taskId={}, progress={}%", task.getId(), progress);
        }

        // Return result
        Map<String, Object> result = new HashMap<>();
        result.put("exportPath", "/exports/data-" + task.getId() + ".csv");
        result.put("rowCount", 10000);
        result.put("fileSize", "2.5MB");

        return result;
    }

    /**
     * Handle data import task
     */
    private Map<String, Object> handleDataImport(Task task) {
        log.info("Handling data import: taskId={}", task.getId());

        // Simulate import work
        simulateWork(3);

        Map<String, Object> result = new HashMap<>();
        result.put("importedCount", 5000);
        result.put("skippedCount", 10);

        return result;
    }

    /**
     * Handle report generation task
     */
    private Map<String, Object> handleReportGeneration(Task task) {
        log.info("Handling report generation: taskId={}", task.getId());

        // Simulate report generation
        simulateWork(5);

        Map<String, Object> result = new HashMap<>();
        result.put("reportUrl", "/reports/" + task.getId() + ".pdf");
        result.put("pageCount", 25);

        return result;
    }

    /**
     * Handle unknown task type
     */
    private Map<String, Object> handleUnknownTask(Task task) {
        log.warn("Unknown task type: taskId={}, taskType={}", task.getId(), task.getTaskType());

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Unknown task type: " + task.getTaskType());

        return result;
    }

    /**
     * Simulate work with random duration
     */
    private void simulateWork(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Task interrupted", e);
        }
    }
}
