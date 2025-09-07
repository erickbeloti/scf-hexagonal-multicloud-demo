package com.example.tasks.application.service;

import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.domain.TaskValidationService;
import com.example.tasks.domain.UserId;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class TaskValidationServiceImpl implements TaskValidationService {

    private final TaskRepositoryPort repository;

    public TaskValidationServiceImpl(TaskRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByUserAndDateAndDescription(UserId userId, LocalDate date, String description) {
        return repository.existsByUserAndDateAndDescription(userId, date, description);
    }

    @Override
    public long countHighPriorityTasksForUserOnDate(UserId userId, LocalDate date) {
        return repository.countHighPriorityTasksForUserOnDate(userId, date);
    }

    @Override
    public long countOpenTasksForUser(UserId userId) {
        return repository.countOpenTasksForUser(userId);
    }
}
