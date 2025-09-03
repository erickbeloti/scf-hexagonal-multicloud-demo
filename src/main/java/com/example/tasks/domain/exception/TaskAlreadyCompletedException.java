package com.example.tasks.domain.exception;

/**
 * Simplified exception for task already completed scenarios
 */
public class TaskAlreadyCompletedException extends TaskBusinessRuleException {

    public TaskAlreadyCompletedException(String message) {
        super(message);
    }

    public TaskAlreadyCompletedException() {
        super("Task is already completed");
    }
}
