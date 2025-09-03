package com.example.tasks.domain.exception;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

/**
 * Simplified domain exception tests for our consolidated exception hierarchy
 */
@DisplayName("Domain Exception Tests")
class DomainExceptionTest {

    @Nested
    @DisplayName("Task Business Rule Exception")
    class TaskBusinessRuleExceptionTests {

        @Test
        @DisplayName("Should create exception with message")
        void shouldCreateExceptionWithMessage() {
            // Given
            String message = "Business rule violated";

            // When
            TaskBusinessRuleException exception = new TaskBusinessRuleException(message);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should create exception with message and cause")
        void shouldCreateExceptionWithMessageAndCause() {
            // Given
            String message = "Business rule violated";
            RuntimeException cause = new RuntimeException("Root cause");

            // When
            TaskBusinessRuleException exception = new TaskBusinessRuleException(message, cause);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("Task Already Completed Exception")
    class TaskAlreadyCompletedExceptionTests {

        @Test
        @DisplayName("Should create exception with default message")
        void shouldCreateExceptionWithDefaultMessage() {
            // When
            TaskAlreadyCompletedException exception = new TaskAlreadyCompletedException();

            // Then
            assertThat(exception.getMessage()).isEqualTo("Task is already completed");
            assertThat(exception).isInstanceOf(TaskBusinessRuleException.class);
        }

        @Test
        @DisplayName("Should create exception with custom message")
        void shouldCreateExceptionWithCustomMessage() {
            // Given
            String message = "Cannot update completed task";

            // When
            TaskAlreadyCompletedException exception = new TaskAlreadyCompletedException(message);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception).isInstanceOf(TaskBusinessRuleException.class);
        }
    }

    @Nested
    @DisplayName("Task Not Found Exception")
    class TaskNotFoundExceptionTests {

        @Test
        @DisplayName("Should create exception with task ID")
        void shouldCreateExceptionWithTaskId() {
            // Given
            com.example.tasks.domain.TaskId taskId = com.example.tasks.domain.TaskId.of("123e4567-e89b-12d3-a456-426614174000");

            // When
            TaskNotFoundException exception = new TaskNotFoundException(taskId);

            // Then
            assertThat(exception.getMessage()).contains("Task not found");
            assertThat(exception.getMessage()).contains(taskId.value());
        }
    }

    @Nested
    @DisplayName("Task Access Denied Exception")
    class TaskAccessDeniedExceptionTests {

        @Test
        @DisplayName("Should create exception with task and user ID")
        void shouldCreateExceptionWithTaskAndUserId() {
            // Given
            com.example.tasks.domain.TaskId taskId = com.example.tasks.domain.TaskId.of("123e4567-e89b-12d3-a456-426614174000");
            com.example.tasks.domain.UserId userId = com.example.tasks.domain.UserId.of("user123");

            // When
            TaskAccessDeniedException exception = new TaskAccessDeniedException(taskId, userId);

            // Then
            assertThat(exception.getMessage()).contains("does not have access");
            assertThat(exception.getMessage()).contains(taskId.value());
            assertThat(exception.getMessage()).contains(userId.value());
        }
    }
}
