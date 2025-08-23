package com.example.tasks.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public record Task(
    String id,
    String userId,
    String description,
    Priority priority,
    Status status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    
    public Task {
        Objects.requireNonNull(id, "Task ID cannot be null");
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
        Objects.requireNonNull(priority, "Priority cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(createdAt, "Created at cannot be null");
        Objects.requireNonNull(updatedAt, "Updated at cannot be null");
        
        if (description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }
        
        if (id.trim().isEmpty()) {
            throw new IllegalArgumentException("Task ID cannot be empty");
        }
        
        if (userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
    }
    
    public Task withStatus(Status newStatus) {
        return new Task(id, userId, description, priority, newStatus, createdAt, updatedAt);
    }
    
    public Task withUpdatedAt(LocalDateTime newUpdatedAt) {
        return new Task(id, userId, description, priority, status, createdAt, newUpdatedAt);
    }
    
    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }
    
    public boolean isOpen() {
        return status != Status.COMPLETED;
    }
    
    public boolean isHighPriority() {
        return priority == Priority.HIGH;
    }
    
    public LocalDate getDate() {
        return createdAt.toLocalDate();
    }
}