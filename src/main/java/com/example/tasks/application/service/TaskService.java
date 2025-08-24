package com.example.tasks.application.service;

import com.example.tasks.application.port.inbound.*;
import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.application.policy.TaskPolicies;
import com.example.tasks.domain.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class TaskService implements CreateTaskUseCase, UpdateTaskUseCase, GetTaskUseCase, ListTasksUseCase, DeleteTaskUseCase {

    private final TaskRepositoryPort repository;
    private final TaskPolicies policies;
    private final Clock clock;

    public TaskService(TaskRepositoryPort repository, TaskPolicies policies, Clock clock) {
        this.repository = repository;
        this.policies = policies;
        this.clock = clock;
    }

    @Override
    public Task createTask(String userId, String description, Priority priority) {
        policies.validateCreation(userId, description, priority);
        LocalDateTime now = LocalDateTime.now(clock);
        Task task = new Task(UUID.randomUUID().toString(), userId, description, priority, Status.OPEN, now, now);
        return repository.save(task);
    }

    @Override
    public Task updateTask(String id, String userId, String description, Priority priority, Status status) {
        Task existing = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Task not found"));
        policies.validateUpdate(existing, userId, description, priority, status);
        LocalDateTime now = LocalDateTime.now(clock);
        Status newStatus = status != null ? status : existing.status();
        Task updated = new Task(id, userId, description, priority, newStatus, existing.createdAt(), now);
        return repository.save(updated);
    }

    @Override
    public Task getTask(String id, String userId) {
        Task task = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Task not found"));
        policies.assertOwnership(task, userId);
        return task;
    }

    @Override
    public List<Task> listTasks(String userId, int page, int size) {
        return repository.listByUser(userId, page, size);
    }

    @Override
    public void deleteTask(String id, String userId) {
        Task task = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Task not found"));
        policies.assertOwnership(task, userId);
        repository.deleteByIdAndUser(id, userId);
    }
}
