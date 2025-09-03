package com.example.tasks.domain;

import java.time.LocalDate;

public interface TaskValidationService {

    boolean existsByUserAndDateAndDescription(UserId userId, LocalDate date, String description);

    long countHighPriorityTasksForUserOnDate(UserId userId, LocalDate date);

    long countOpenTasksForUser(UserId userId);
}
