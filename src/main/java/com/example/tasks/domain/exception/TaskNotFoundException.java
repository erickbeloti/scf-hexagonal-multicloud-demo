package com.example.tasks.domain.exception;

import com.example.tasks.domain.TaskId;

/**
 * Domain exception thrown when a task is not found.
 */
public class TaskNotFoundException extends TaskDomainException {

    public TaskNotFoundException(TaskId taskId) {
        super("TASK_NOT_FOUND",
              "Task not found with ID: " + taskId.value());
    }

    public TaskNotFoundException(String message) {
        super("TASK_NOT_FOUND", message);
    }
}
