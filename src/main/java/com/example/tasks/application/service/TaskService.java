package com.example.tasks.application.service;

import com.example.tasks.application.port.inbound.CreateTaskUseCase;
import com.example.tasks.application.port.inbound.DeleteTaskUseCase;
import com.example.tasks.application.port.inbound.GetTaskUseCase;
import com.example.tasks.application.port.inbound.ListTasksUseCase;
import com.example.tasks.application.port.inbound.UpdateTaskUseCase;
import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.domain.Priority;
import com.example.tasks.domain.Status;
import com.example.tasks.domain.Task;
import com.example.tasks.domain.TaskId;
import com.example.tasks.domain.TaskValidationService;
import com.example.tasks.domain.UserId;
import com.example.tasks.domain.exception.TaskNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService implements CreateTaskUseCase, UpdateTaskUseCase, GetTaskUseCase, ListTasksUseCase, DeleteTaskUseCase {

    private final TaskRepositoryPort repository;
    private final TaskValidationService validationService;
    private final Clock clock;

    public TaskService(TaskRepositoryPort repository, TaskValidationService validationService, Clock clock) {
        this.repository = repository;
        this.validationService = validationService;
        this.clock = clock;
    }

    @Override
    public Task createTask(UserId userId, String description, Priority priority) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();

        // Business rules validated using domain service interface
        Task.validateCreationRules(userId, description, priority, today, validationService);

        // Create and save task
        TaskId taskId = TaskId.generate();
        Task task = new Task(taskId, userId, description, priority, now);

        return repository.save(task);
    }

    @Override
    public Task updateTask(TaskId id, UserId userId, String description, Priority priority, Status status) {
        Task existingTask = repository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));

        // Ensure user can only update their own tasks
        existingTask.ensureOwnership(userId);

        LocalDateTime now = LocalDateTime.now(clock);
        Task updatedTask = existingTask;

        // Apply updates using Task entity methods
        if (description != null && !description.equals(existingTask.getDescription())) {
            updatedTask = updatedTask.updateDescription(description, now);
        }

        if (priority != null && priority != existingTask.getPriority()) {
            updatedTask = updatedTask.changePriority(priority, now);
        }

        if (status != null && status != existingTask.getStatus()) {
            if (status == Status.COMPLETED) {
                updatedTask = updatedTask.complete(now);
            }
        }

        return repository.save(updatedTask);
    }

    @Override
    public Task getTask(TaskId id, UserId userId) {
        Task task = repository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));

        // Ensure user can only access their own tasks
        task.ensureOwnership(userId);

        return task;
    }

    @Override
    public List<Task> listTasks(UserId userId, int page, int size) {
        return repository.findByUserId(userId, page, size);
    }

    @Override
    public void deleteTask(TaskId id, UserId userId) {
        Task task = repository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));

        // Ensure user can only delete their own tasks
        task.ensureOwnership(userId);

        repository.deleteById(id);
    }
}
