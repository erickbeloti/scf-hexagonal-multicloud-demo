package com.example.tasks.domain.exception;

/**
 * Simplified exception hierarchy - single exception for all business rule violations
 */
public class TaskBusinessRuleException extends RuntimeException {

    public TaskBusinessRuleException(String message) {
        super(message);
    }

    public TaskBusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
