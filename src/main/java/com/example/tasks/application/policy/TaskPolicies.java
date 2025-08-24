package com.example.tasks.application.policy;

import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.domain.*;
import java.time.*;
import org.springframework.stereotype.Component;

@Component
public class TaskPolicies {

    private final TaskRepositoryPort repository;
    private final Clock clock;

    public TaskPolicies(TaskRepositoryPort repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void validateCreation(String userId, String description, Priority priority) {
        LocalDate today = LocalDate.now(clock);
        repository.findByUserAndDateAndDescription(userId, today, description)
            .ifPresent(t -> { throw new IllegalStateException("Description must be unique per day"); });
        if (priority == Priority.HIGH && repository.countHighPriorityForUserOn(userId, today) >= 5) {
            throw new IllegalStateException("Too many high priority tasks for today");
        }
        if (repository.countOpenByUser(userId) >= 50) {
            throw new IllegalStateException("Too many open tasks");
        }
    }

    public void validateUpdate(Task existing, String userId, String description, Priority priority, Status status) {
        assertOwnership(existing, userId);
        if (existing.status() == Status.COMPLETED) {
            throw new IllegalStateException("Completed task cannot be updated");
        }
        LocalDate day = existing.createdAt().toLocalDate();
        if (!existing.description().equals(description)
                && repository.findByUserAndDateAndDescription(userId, day, description).isPresent()) {
            throw new IllegalStateException("Description must be unique per day");
        }
        if (priority == Priority.HIGH && existing.priority() != Priority.HIGH
                && repository.countHighPriorityForUserOn(userId, day) >= 5) {
            throw new IllegalStateException("Too many high priority tasks for today");
        }
    }

    public void assertOwnership(Task task, String userId) {
        if (!task.userId().equals(userId)) {
            throw new IllegalStateException("Forbidden");
        }
    }
}

