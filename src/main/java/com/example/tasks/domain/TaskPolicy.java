package com.example.tasks.domain;

import java.time.LocalDate;
import java.util.List;

public class TaskPolicy {
    
    private static final int MAX_HIGH_PRIORITY_TASKS_PER_DAY = 5;
    private static final int MAX_OPEN_TASKS_PER_USER = 50;
    
    public void validateCreateTask(String userId, String description, LocalDate date, 
                                 List<Task> existingTasks) {
        validateDescriptionUniqueness(userId, description, date, existingTasks);
        validateHighPriorityLimit(userId, date, existingTasks);
        validateOpenTasksLimit(userId, existingTasks);
    }
    
    public void validateUpdateTask(Task existingTask, List<Task> userTasks) {
        if (existingTask.isCompleted()) {
            throw new TaskException("Cannot update completed tasks");
        }
        
        // For updates, we need to check limits considering the existing task
        // Remove the existing task from the count for validation
        List<Task> otherTasks = userTasks.stream()
            .filter(task -> !task.id().equals(existingTask.id()))
            .toList();
            
        validateOpenTasksLimit(existingTask.userId(), otherTasks);
    }
    
    public void validateDeleteTask(Task task, String requestingUserId) {
        if (!task.userId().equals(requestingUserId)) {
            throw new TaskException("Only task owner can delete the task");
        }
    }
    
    public void validateGetTask(Task task, String requestingUserId) {
        if (!task.userId().equals(requestingUserId)) {
            throw new TaskException("Only task owner can view the task");
        }
    }
    
    private void validateDescriptionUniqueness(String userId, String description, 
                                            LocalDate date, List<Task> existingTasks) {
        boolean descriptionExists = existingTasks.stream()
            .anyMatch(task -> task.userId().equals(userId) && 
                            task.getDate().equals(date) && 
                            task.description().equals(description));
        
        if (descriptionExists) {
            throw new TaskException("Description must be unique per user per day");
        }
    }
    
    private void validateHighPriorityLimit(String userId, LocalDate date, 
                                        List<Task> existingTasks) {
        long highPriorityCount = existingTasks.stream()
            .filter(task -> task.userId().equals(userId) && 
                          task.getDate().equals(date) && 
                          task.isHighPriority())
            .count();
        
        if (highPriorityCount >= MAX_HIGH_PRIORITY_TASKS_PER_DAY) {
            throw new TaskException("Maximum of " + MAX_HIGH_PRIORITY_TASKS_PER_DAY + 
                                  " high priority tasks allowed per day per user");
        }
    }
    
    private void validateOpenTasksLimit(String userId, List<Task> existingTasks) {
        long openTasksCount = existingTasks.stream()
            .filter(task -> task.userId().equals(userId) && task.isOpen())
            .count();
        
        if (openTasksCount >= MAX_OPEN_TASKS_PER_USER) {
            throw new TaskException("Maximum of " + MAX_OPEN_TASKS_PER_USER + 
                                  " open tasks allowed per user");
        }
    }
}