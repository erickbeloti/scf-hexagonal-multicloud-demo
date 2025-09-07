package com.example.tasks.domain.exception;

/**
 * Base exception for all domain-related exceptions in the task management domain.
 * This serves as the root exception for all business rule violations and domain errors.
 */
public abstract class TaskDomainException extends RuntimeException {

    private final String errorCode;

    protected TaskDomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected TaskDomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
