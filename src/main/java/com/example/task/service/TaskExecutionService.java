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

    @Value("${task.processing.failed-rate:0.0}")
    private double failedRate;

    // Track active tasks for monitoring concurrency
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    /**
     * Execute task based on task type
     */
    public Map<String, Object> execute(Task task) {
        int currentActive = activeTasks.incrementAndGet();
        log.info("Task started: taskId={}, taskType={}, activeTasks={}", task.getId(), task.getTaskType(), currentActive);

        try {
            // Simulate random failure based on configured rate
            if (shouldFail()) {
                activeTasks.decrementAndGet();
                log.warn("Task failed (simulated): taskId={}, failedRate={}", task.getId(), failedRate);
                throw new RuntimeException("Simulated task failure for testing");
            }

            Map<String, Object> result = handleDataExport(task);

            log.info("Task executed successfully: taskId={}, activeTasks={}", task.getId(), activeTasks.decrementAndGet());
            return result;

        } catch (RuntimeException e) {
            // Re-throw RuntimeException as-is (includes our simulated failure)
            throw e;
        } catch (Exception e) {
            activeTasks.decrementAndGet();
            log.error("Task execution failed: taskId={}", task.getId(), e);
            throw new RuntimeException("Task execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Determine if task should fail based on configured failure rate
     */
    private boolean shouldFail() {
        if (failedRate <= 0.0) {
            return false;
        }
        if (failedRate >= 1.0) {
            return true;
        }
        return random.nextDouble() < failedRate;
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

}
