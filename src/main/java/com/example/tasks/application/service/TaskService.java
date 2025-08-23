package com.example.tasks.application.service;

import com.example.tasks.application.port.inbound.CreateTaskUseCase;
import com.example.tasks.application.port.inbound.DeleteTaskUseCase;
import com.example.tasks.application.port.inbound.GetTaskByIdUseCase;
import com.example.tasks.application.port.inbound.ListTasksByUserUseCase;
import com.example.tasks.application.port.inbound.UpdateTaskUseCase;
import com.example.tasks.application.port.outbound.ClockPort;
import com.example.tasks.application.port.outbound.IdGeneratorPort;
import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.domain.model.Priority;
import com.example.tasks.domain.model.Status;
import com.example.tasks.domain.model.Task;
import com.example.tasks.domain.policy.TaskCreationPolicy;
import com.example.tasks.domain.policy.TaskUpdatePolicy;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

public class TaskService implements CreateTaskUseCase, UpdateTaskUseCase,
        GetTaskByIdUseCase, ListTasksByUserUseCase, DeleteTaskUseCase {

    private final TaskRepositoryPort repository;
    private final ClockPort clockPort;
    private final IdGeneratorPort idGeneratorPort;
    private final TaskCreationPolicy creationPolicy;
    private final TaskUpdatePolicy updatePolicy;

    public TaskService(TaskRepositoryPort repository,
                       ClockPort clockPort,
                       IdGeneratorPort idGeneratorPort,
                       TaskCreationPolicy creationPolicy,
                       TaskUpdatePolicy updatePolicy) {
        this.repository = repository;
        this.clockPort = clockPort;
        this.idGeneratorPort = idGeneratorPort;
        this.creationPolicy = creationPolicy;
        this.updatePolicy = updatePolicy;
    }

    @Override
    public Task create(Task draft) {
        Instant now = Instant.now(clockPort.systemClock());
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        long numHighToday = repository.countHighPriorityForUserOn(today, draft.userId());
        boolean unique = repository
                .findByUserAndDateAndDescription(draft.userId(), today, draft.description())
                .isEmpty();
        long numOpen = repository.countOpenByUser(draft.userId());
        creationPolicy.validate(draft.priority(), numHighToday, unique, numOpen);

        String id = draft.id() != null ? draft.id() : idGeneratorPort.newId();
        Task toSave = new Task(
                id,
                draft.userId(),
                draft.description(),
                draft.priority() == null ? Priority.LOW : draft.priority(),
                draft.status() == null ? Status.OPEN : draft.status(),
                now,
                now
        );
        return repository.save(toSave);
    }

    @Override
    public Task update(String id, String userId, Task update) {
        Optional<Task> existingOpt = repository.findById(id);
        if (existingOpt.isEmpty() || !existingOpt.get().userId().equals(userId)) {
            return null; // ownership enforced by caller for read; return null for not found or not owner
        }
        Task existing = existingOpt.get();
        updatePolicy.validateUpdatable(existing);
        Instant now = Instant.now(clockPort.systemClock());
        Task merged = new Task(
                existing.id(),
                existing.userId(),
                update.description() != null ? update.description() : existing.description(),
                update.priority() != null ? update.priority() : existing.priority(),
                update.status() != null ? update.status() : existing.status(),
                existing.createdAt(),
                now
        );
        return repository.save(merged);
    }

    @Override
    public Optional<Task> get(String id, String userId) {
        return repository.findById(id).filter(t -> t.userId().equals(userId));
    }

    @Override
    public List<Task> list(String userId, int page, int size) {
        return repository.listByUser(userId, page, size);
    }

    @Override
    public boolean delete(String id, String userId) {
        return repository.deleteByIdAndUser(id, userId);
    }
}

