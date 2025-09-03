package com.example.tasks.domain.exception;

import com.example.tasks.domain.TaskId;
import com.example.tasks.domain.UserId;

/**
 * Domain exception thrown when a user tries to access a task they don't own.
 */
public class TaskAccessDeniedException extends TaskDomainException {

    public TaskAccessDeniedException(TaskId taskId, UserId userId) {
        super("TASK_ACCESS_DENIED",
              "User " + userId.value() + " does not have access to task " + taskId.value());
    }

    public TaskAccessDeniedException(String message) {
        super("TASK_ACCESS_DENIED", message);
    }
}
