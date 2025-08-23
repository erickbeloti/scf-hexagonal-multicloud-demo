package com.example.tasks.adapters.outbound.local;

import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.domain.model.Priority;
import com.example.tasks.domain.model.Status;
import com.example.tasks.domain.model.Task;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Profile("local")
public class InMemoryTaskRepository implements TaskRepositoryPort {
    private final Map<String, Task> storage = new ConcurrentHashMap<>();

    @Override
    public Task save(Task task) {
        storage.put(task.id(), task);
        return task;
    }

    @Override
    public Optional<Task> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<Task> findByUserAndDateAndDescription(String userId, LocalDate date, String description) {
        return storage.values().stream()
                .filter(t -> t.userId().equals(userId)
                        && toDate(t.createdAt()).equals(date)
                        && t.description().equalsIgnoreCase(description))
                .findFirst();
    }

    @Override
    public long countHighPriorityForUserOn(LocalDate date, String userId) {
        return storage.values().stream()
                .filter(t -> t.userId().equals(userId)
                        && toDate(t.createdAt()).equals(date)
                        && t.priority() == Priority.HIGH)
                .count();
    }

    @Override
    public long countOpenByUser(String userId) {
        return storage.values().stream()
                .filter(t -> t.userId().equals(userId) && t.status() != Status.COMPLETED)
                .count();
    }

    @Override
    public List<Task> listByUser(String userId, int page, int size) {
        return storage.values().stream()
                .filter(t -> t.userId().equals(userId))
                .sorted(Comparator.comparing(Task::createdAt).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteByIdAndUser(String id, String userId) {
        Task t = storage.get(id);
        if (t != null && t.userId().equals(userId)) {
            storage.remove(id);
            return true;
        }
        return false;
    }

    private LocalDate toDate(Instant instant) {
        return LocalDate.ofInstant(instant, ZoneOffset.UTC);
    }
}

