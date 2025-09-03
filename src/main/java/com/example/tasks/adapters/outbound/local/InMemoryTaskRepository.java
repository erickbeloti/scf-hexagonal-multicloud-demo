package com.example.tasks.adapters.outbound.local;

import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.domain.Task;
import com.example.tasks.domain.TaskId;
import com.example.tasks.domain.UserId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
@Profile("local")
public class InMemoryTaskRepository implements TaskRepositoryPort {

    private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    @Override
    public Task save(Task task) {
        tasks.put(task.getId().value(), task);
        return task;
    }

    @Override
    public Optional<Task> findById(TaskId id) {
        return Optional.ofNullable(tasks.get(id.value()));
    }

    @Override
    public List<Task> findByUserId(UserId userId, int page, int size) {
        return tasks.values().stream()
                .filter(task -> task.belongsTo(userId))
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByUserAndDateAndDescription(UserId userId, LocalDate date, String description) {
        return tasks.values().stream()
                .anyMatch(task ->
                    task.belongsTo(userId) &&
                    task.wasCreatedOn(date) &&
                    task.getDescription().equals(description)
                );
    }

    @Override
    public long countHighPriorityTasksForUserOnDate(UserId userId, LocalDate date) {
        return tasks.values().stream()
                .filter(task ->
                    task.belongsTo(userId) &&
                    task.wasCreatedOn(date) &&
                    task.isHighPriority()
                )
                .count();
    }

    @Override
    public long countOpenTasksForUser(UserId userId) {
        return tasks.values().stream()
                .filter(task -> task.belongsTo(userId) && task.isOpen())
                .count();
    }

    @Override
    public void deleteById(TaskId id) {
        tasks.remove(id.value());
    }

    public void clear() {
        tasks.clear();
    }

    public int size() {
        return tasks.size();
    }
}
