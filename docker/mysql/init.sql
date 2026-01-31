-- MySQL Initialization Script for Task Database

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS task_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE task_db;

-- Create tasks table
CREATE TABLE IF NOT EXISTS tasks (
    id VARCHAR(64) PRIMARY KEY COMMENT 'Task ID (UUID)',
    task_type VARCHAR(32) NOT NULL COMMENT 'Task type',
    status VARCHAR(16) NOT NULL COMMENT 'PENDING, RUNNING, SUCCESS, FAILED',
    params JSON COMMENT 'Task parameters (JSON)',
    result JSON COMMENT 'Task result (JSON, only when SUCCESS)',
    error_message TEXT COMMENT 'Error message (only when FAILED)',
    progress INT DEFAULT 0 COMMENT 'Progress: 0-100',
    retry_count INT DEFAULT 0 COMMENT 'Retry count',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    started_at TIMESTAMP NULL COMMENT 'Start time',
    finished_at TIMESTAMP NULL COMMENT 'Finish time',
    INDEX idx_status (status),
    INDEX idx_task_type (task_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Async tasks table';

-- Insert sample data (optional)
-- INSERT INTO tasks (id, task_type, status, params) VALUES
-- ('sample-task-1', 'DATA_EXPORT', 'PENDING', '{"userId": "123"}');

-- Grant privileges (if needed)
-- GRANT ALL PRIVILEGES ON task_db.* TO 'task_user'@'%';
-- FLUSH PRIVILEGES;
