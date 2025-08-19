package com.example.tasks.domain.port.out;

import com.example.tasks.domain.model.Task;

import java.util.Optional;

public interface TaskRepository {
    Task createTask(Task task);
    Optional<Task> getTask(String id);
    Task updateTask(Task task);
    void deleteTask(String id);
}
