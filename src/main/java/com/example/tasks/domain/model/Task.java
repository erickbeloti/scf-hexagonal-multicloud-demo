package com.example.tasks.domain.model;

import java.time.Instant;
import java.util.Objects;

public record Task(
        String id,
        String userId,
        String description,
        Priority priority,
        Status status,
        Instant createdAt,
        Instant updatedAt
) {
    public Task {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }
}

