package com.example.tasks.domain;

public record UserId(String value) {

    public UserId {
        if (value == null) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("User ID cannot exceed 100 characters");
        }
        value = trimmed;
    }

    public static UserId of(String value) {
        return new UserId(value);
    }
}
