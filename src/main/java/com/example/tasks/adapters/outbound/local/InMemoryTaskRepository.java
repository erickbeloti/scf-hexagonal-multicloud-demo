package com.example.tasks.adapters.outbound.local;

import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.domain.Task;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
@Profile("local")
public class InMemoryTaskRepository implements TaskRepositoryPort {
    
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    
    @Override
    public Task save(Task task) {
        tasks.put(task.id(), task);
        return task;
    }
    
    @Override
    public Optional<Task> findById(String id) {
        return Optional.ofNullable(tasks.get(id));
    }
    
    @Override
    public Optional<Task> findByUserAndDateAndDescription(String userId, LocalDate date, String description) {
        return tasks.values().stream()
            .filter(task -> task.userId().equals(userId) && 
                          task.getDate().equals(date) && 
                          task.description().equals(description))
            .findFirst();
    }
    
    @Override
    public long countHighPriorityForUserOn(LocalDate date, String userId) {
        return tasks.values().stream()
            .filter(task -> task.userId().equals(userId) && 
                          task.getDate().equals(date) && 
                          task.isHighPriority())
            .count();
    }
    
    @Override
    public long countOpenByUser(String userId) {
        return tasks.values().stream()
            .filter(task -> task.userId().equals(userId) && task.isOpen())
            .count();
    }
    
    @Override
    public List<Task> listByUser(String userId, int page, int size) {
        List<Task> userTasks = tasks.values().stream()
            .filter(task -> task.userId().equals(userId))
            .sorted(Comparator.comparing(Task::createdAt).reversed())
            .collect(Collectors.toList());
        
        int start = page * size;
        int end = Math.min(start + size, userTasks.size());
        
        if (start >= userTasks.size()) {
            return List.of();
        }
        
        return userTasks.subList(start, end);
    }
    
    @Override
    public void deleteByIdAndUser(String id, String userId) {
        Task task = tasks.get(id);
        if (task != null && task.userId().equals(userId)) {
            tasks.remove(id);
        }
    }
}