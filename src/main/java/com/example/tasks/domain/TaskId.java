package com.example.tasks.domain;

import java.util.UUID;

public record TaskId(String value) {

    public TaskId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Task ID cannot be null or empty");
        }

        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Task ID must be a valid UUID format", e);
        }
    }

    public static TaskId generate() {
        return new TaskId(UUID.randomUUID().toString());
    }

    public static TaskId of(String value) {
        return new TaskId(value);
    }
}
