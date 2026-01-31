package com.example.task.repository;

import com.example.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Task Repository
 * Spring Data JPA repository for Task entity
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, String> {
    // Spring Data JPA automatically provides CRUD methods:
    // - save(): insert or update
    // - findById(): query by primary key
    // - findAll(): query all records
    // - deleteById(): delete by primary key
    // - count(): count records
}
