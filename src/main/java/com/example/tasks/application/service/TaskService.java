package com.example.tasks.application.service;

import com.example.tasks.application.port.in.CreateTaskUseCase;
import com.example.tasks.application.port.in.GetTaskUseCase;
import com.example.tasks.application.port.in.UpdateTaskUseCase;
import com.example.tasks.application.port.in.DeleteTaskUseCase;
import com.example.tasks.domain.model.Task;
import com.example.tasks.domain.port.out.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TaskService implements CreateTaskUseCase, GetTaskUseCase, UpdateTaskUseCase, DeleteTaskUseCase {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public Task createTask(Task task) {
        return taskRepository.createTask(task);
    }

    @Override
    public Optional<Task> getTask(String id) {
        return taskRepository.getTask(id);
    }

    @Override
    public Task updateTask(Task task) {
        return taskRepository.updateTask(task);
    }

    @Override
    public void deleteTask(String id) {
        taskRepository.deleteTask(id);
    }
}
