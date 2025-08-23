package com.example.tasks.application.service;

import com.example.tasks.application.port.inbound.*;
import com.example.tasks.application.port.outbound.*;
import com.example.tasks.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TaskService implements CreateTaskUseCase, UpdateTaskUseCase, GetTaskUseCase, 
                                  ListTasksUseCase, DeleteTaskUseCase {
    
    private final TaskRepositoryPort taskRepository;
    private final ClockPort clock;
    private final IdGeneratorPort idGenerator;
    private final TaskPolicy taskPolicy;
    
    public TaskService(TaskRepositoryPort taskRepository, ClockPort clock, 
                      IdGeneratorPort idGenerator, TaskPolicy taskPolicy) {
        this.taskRepository = taskRepository;
        this.clock = clock;
        this.idGenerator = idGenerator;
        this.taskPolicy = taskPolicy;
    }
    
    @Override
    public Task createTask(String userId, String description, String priority) {
        LocalDateTime now = clock.now();
        LocalDate date = now.toLocalDate();
        
        // Get existing tasks for validation
        List<Task> existingTasks = taskRepository.listByUser(userId, 0, Integer.MAX_VALUE);
        
        // Validate business rules
        taskPolicy.validateCreateTask(userId, description, date, existingTasks);
        
        // Create task
        Task task = new Task(
            idGenerator.generateId(),
            userId,
            description,
            Priority.valueOf(priority.toUpperCase()),
            Status.PENDING,
            now,
            now
        );
        
        return taskRepository.save(task);
    }
    
    @Override
    public Task updateTask(String taskId, String userId, String description, 
                          String priority, String status) {
        Task existingTask = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskException("Task not found"));
        
        // Validate ownership
        if (!existingTask.userId().equals(userId)) {
            throw new TaskException("Only task owner can update the task");
        }
        
        // Get user's tasks for validation
        List<Task> userTasks = taskRepository.listByUser(userId, 0, Integer.MAX_VALUE);
        
        // Validate business rules
        taskPolicy.validateUpdateTask(existingTask, userTasks);
        
        // Update task
        Task updatedTask = new Task(
            existingTask.id(),
            existingTask.userId(),
            description,
            Priority.valueOf(priority.toUpperCase()),
            Status.valueOf(status.toUpperCase()),
            existingTask.createdAt(),
            clock.now()
        );
        
        return taskRepository.save(updatedTask);
    }
    
    @Override
    public Task getTask(String taskId, String userId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskException("Task not found"));
        
        taskPolicy.validateGetTask(task, userId);
        return task;
    }
    
    @Override
    public List<Task> listTasks(String userId, int page, int size) {
        return taskRepository.listByUser(userId, page, size);
    }
    
    @Override
    public void deleteTask(String taskId, String userId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskException("Task not found"));
        
        taskPolicy.validateDeleteTask(task, userId);
        taskRepository.deleteByIdAndUser(taskId, userId);
    }
}