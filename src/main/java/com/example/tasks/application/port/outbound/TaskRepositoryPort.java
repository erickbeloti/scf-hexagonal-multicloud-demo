package com.example.tasks.application.port.outbound;

import com.example.tasks.domain.Task;
import com.example.tasks.domain.TaskId;
import com.example.tasks.domain.UserId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepositoryPort {
    // Core persistence operations
    Task save(Task task);
    Optional<Task> findById(TaskId id);
    List<Task> findByUserId(UserId userId, int page, int size);
    void deleteById(TaskId id);

    // Business rule queries (needed for domain validation)
    boolean existsByUserAndDateAndDescription(UserId userId, LocalDate date, String description);
    long countHighPriorityTasksForUserOnDate(UserId userId, LocalDate date);
    long countOpenTasksForUser(UserId userId);
}
