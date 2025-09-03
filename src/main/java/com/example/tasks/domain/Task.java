package com.example.tasks.domain;

import com.example.tasks.domain.exception.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Task aggregate root with embedded business rules.
 * Implements DDD patterns with rich domain model and behavior.
 */
public class Task {
    private final TaskId id;
    private final UserId userId;
    private final String description;
    private final Priority priority;
    private final Status status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Task(TaskId id, UserId userId, String description, Priority priority, LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "Task ID cannot be null");
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.description = validateDescription(description);
        this.priority = Objects.requireNonNull(priority, "Priority cannot be null");
        this.status = Status.OPEN;
        this.createdAt = Objects.requireNonNull(createdAt, "Created date cannot be null");
        this.updatedAt = createdAt;
    }

    private Task(TaskId id, UserId userId, String description, Priority priority, Status status,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Task reconstitute(TaskId id, UserId userId, String description, Priority priority,
                                   Status status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Task(id, userId, description, priority, status, createdAt, updatedAt);
    }

    public static void validateCreationRules(UserId userId, String description, Priority priority,
                                           LocalDate date, TaskValidationService validationService) {
        if (validationService.existsByUserAndDateAndDescription(userId, date, description)) {
            throw new TaskBusinessRuleException("Description must be unique per user per day");
        }

        if (priority.isHighPriority() &&
            validationService.countHighPriorityTasksForUserOnDate(userId, date) >= TaskBusinessRules.MAX_HIGH_PRIORITY_TASKS_PER_DAY) {
            throw new TaskBusinessRuleException("Cannot create more than " + TaskBusinessRules.MAX_HIGH_PRIORITY_TASKS_PER_DAY + " high priority tasks per day");
        }

        if (validationService.countOpenTasksForUser(userId) >= TaskBusinessRules.MAX_OPEN_TASKS_PER_USER) {
            throw new TaskBusinessRuleException("Cannot have more than " + TaskBusinessRules.MAX_OPEN_TASKS_PER_USER + " open tasks");
        }
    }

    public Task updateDescription(String newDescription, LocalDateTime updatedTime) {
        if (status == Status.COMPLETED) {
            throw new TaskBusinessRuleException("Cannot update completed task");
        }

        String cleanDescription = validateDescription(newDescription);
        if (cleanDescription.equals(this.description)) {
            return this;
        }

        return new Task(id, userId, cleanDescription, priority, status, createdAt, updatedTime);
    }

    public Task changePriority(Priority newPriority, LocalDateTime updatedTime) {
        if (status == Status.COMPLETED) {
            throw new TaskBusinessRuleException("Cannot update completed task");
        }

        if (newPriority == this.priority) {
            return this;
        }

        return new Task(id, userId, description, newPriority, status, createdAt, updatedTime);
    }

    public Task complete(LocalDateTime completedTime) {
        if (status == Status.COMPLETED) {
            throw new TaskBusinessRuleException("Task is already completed");
        }
        return new Task(id, userId, description, priority, Status.COMPLETED, createdAt, completedTime);
    }

    public void ensureOwnership(UserId requestingUserId) {
        if (!this.userId.equals(requestingUserId)) {
            throw new TaskBusinessRuleException("User can only access their own tasks");
        }
    }

    private String validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new TaskBusinessRuleException("Description cannot be empty");
        }

        String cleaned = description.trim();
        if (cleaned.length() < TaskBusinessRules.MIN_DESCRIPTION_LENGTH) {
            throw new TaskBusinessRuleException("Description must be at least " + TaskBusinessRules.MIN_DESCRIPTION_LENGTH + " characters");
        }

        if (cleaned.length() > TaskBusinessRules.MAX_DESCRIPTION_LENGTH) {
            throw new TaskBusinessRuleException("Description cannot exceed " + TaskBusinessRules.MAX_DESCRIPTION_LENGTH + " characters");
        }

        return cleaned;
    }

    public boolean belongsTo(UserId userId) {
        return this.userId.equals(userId);
    }

    public boolean isHighPriority() {
        return this.priority.isHighPriority();
    }

    public boolean isOpen() {
        return this.status == Status.OPEN;
    }

    public boolean wasCreatedOn(LocalDate date) {
        return this.createdAt.toLocalDate().equals(date);
    }

    // Getters
    public TaskId getId() { return id; }
    public UserId getUserId() { return userId; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public Status getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
