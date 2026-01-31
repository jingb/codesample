package com.example.task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application Class
 * Async Task Processing Service with RocketMQ
 */
@SpringBootApplication
public class TaskAsyncServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskAsyncServiceApplication.class, args);
    }
}
