package com.example.tasks.application.service;

import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.domain.Priority;
import com.example.tasks.domain.Status;
import com.example.tasks.domain.Task;
import com.example.tasks.domain.TaskBusinessRules;
import com.example.tasks.domain.TaskId;
import com.example.tasks.domain.TaskValidationService;
import com.example.tasks.domain.UserId;
import com.example.tasks.domain.exception.TaskBusinessRuleException;
import com.example.tasks.domain.exception.TaskNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskServiceTest {

    TaskRepositoryPort repository;
    TaskValidationService validationService;
    Clock clock;
    TaskService service;

    UserId userId;
    UserId otherUserId;
    TaskId taskId;
    LocalDateTime now;
    LocalDate today;

    @BeforeEach
    void setup() {
        repository = mock(TaskRepositoryPort.class);
        validationService = mock(TaskValidationService.class);
        clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new TaskService(repository, validationService, clock);

        userId = UserId.of("user123");
        otherUserId = UserId.of("otherUser");
        taskId = TaskId.of("123e4567-e89b-12d3-a456-426614174000");
        now = LocalDateTime.now(clock);
        today = now.toLocalDate();
    }

    @Nested
    @DisplayName("Create Task Business Rules")
    class CreateTaskTests {

        @Test
        @DisplayName("Should create task successfully when all rules are satisfied")
        void shouldCreateTaskSuccessfully() {
            // Given
            String description = "Test task";
            Priority priority = Priority.MEDIUM;

            // Mock validation service to return valid state
            when(validationService.existsByUserAndDateAndDescription(userId, today, description)).thenReturn(false);
            when(validationService.countHighPriorityTasksForUserOnDate(userId, today)).thenReturn(0L);
            when(validationService.countOpenTasksForUser(userId)).thenReturn(0L);

            // Mock repository to return the saved task
            when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Task result = service.createTask(userId, description, priority);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getDescription()).isEqualTo(description);
            assertThat(result.getPriority()).isEqualTo(priority);
            assertThat(result.getStatus()).isEqualTo(Status.OPEN);

            verify(repository).save(any(Task.class));
        }

        @Test
        @DisplayName("Should fail when high priority limit exceeded")
        void shouldFailWhenTooManyHighPriorityTasks() {
            // Given
            String description = "High priority task";
            Priority priority = Priority.HIGH;

            when(validationService.existsByUserAndDateAndDescription(userId, today, description)).thenReturn(false);
            when(validationService.countHighPriorityTasksForUserOnDate(userId, today))
                .thenReturn((long) TaskBusinessRules.MAX_HIGH_PRIORITY_TASKS_PER_DAY);
            when(validationService.countOpenTasksForUser(userId)).thenReturn(0L);

            // When & Then
            assertThatThrownBy(() -> service.createTask(userId, description, priority))
                .isInstanceOf(TaskBusinessRuleException.class)
                .hasMessageContaining("Cannot create more than " + TaskBusinessRules.MAX_HIGH_PRIORITY_TASKS_PER_DAY);
        }

        @Test
        @DisplayName("Should fail when description is not unique")
        void shouldFailWhenDescriptionNotUnique() {
            // Given
            String description = "Duplicate task";
            Priority priority = Priority.MEDIUM;

            when(validationService.existsByUserAndDateAndDescription(userId, today, description)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> service.createTask(userId, description, priority))
                .isInstanceOf(TaskBusinessRuleException.class)
                .hasMessageContaining("Description must be unique per user per day");
        }

        @Test
        @DisplayName("Should fail when open task limit exceeded")
        void shouldFailWhenTooManyOpenTasks() {
            // Given
            String description = "New task";
            Priority priority = Priority.MEDIUM;

            when(validationService.existsByUserAndDateAndDescription(userId, today, description)).thenReturn(false);
            when(validationService.countHighPriorityTasksForUserOnDate(userId, today)).thenReturn(0L);
            when(validationService.countOpenTasksForUser(userId))
                .thenReturn((long) TaskBusinessRules.MAX_OPEN_TASKS_PER_USER);

            // When & Then
            assertThatThrownBy(() -> service.createTask(userId, description, priority))
                .isInstanceOf(TaskBusinessRuleException.class)
                .hasMessageContaining("Cannot have more than " + TaskBusinessRules.MAX_OPEN_TASKS_PER_USER);
        }
    }

    @Nested
    @DisplayName("Update Task Tests")
    class UpdateTaskTests {

        @Test
        @DisplayName("Should update task description successfully")
        void shouldUpdateDescription() {
            // Given
            Task existingTask = new Task(taskId, userId, "Old description", Priority.MEDIUM, now.minusHours(1));
            String newDescription = "New description";

            when(repository.findById(taskId)).thenReturn(Optional.of(existingTask));
            when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Task result = service.updateTask(taskId, userId, newDescription, null, null);

            // Then
            assertThat(result.getDescription()).isEqualTo(newDescription);
            verify(repository).save(any(Task.class));
        }

        @Test
        @DisplayName("Should update task priority successfully")
        void shouldUpdatePriority() {
            // Given
            Task existingTask = new Task(taskId, userId, "Test task", Priority.LOW, now.minusHours(1));
            Priority newPriority = Priority.HIGH;

            when(repository.findById(taskId)).thenReturn(Optional.of(existingTask));
            when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Task result = service.updateTask(taskId, userId, null, newPriority, null);

            // Then
            assertThat(result.getPriority()).isEqualTo(newPriority);
            verify(repository).save(any(Task.class));
        }

        @Test
        @DisplayName("Should complete task successfully")
        void shouldCompleteTask() {
            // Given
            Task existingTask = new Task(taskId, userId, "Test task", Priority.MEDIUM, now.minusHours(1));

            when(repository.findById(taskId)).thenReturn(Optional.of(existingTask));
            when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Task result = service.updateTask(taskId, userId, null, null, Status.COMPLETED);

            // Then
            assertThat(result.getStatus()).isEqualTo(Status.COMPLETED);
            verify(repository).save(any(Task.class));
        }

        @Test
        @DisplayName("Should fail when user tries to update another user's task")
        void shouldFailWhenUpdatingOtherUserTask() {
            // Given
            Task existingTask = new Task(taskId, otherUserId, "Other user task", Priority.MEDIUM, now.minusHours(1));

            when(repository.findById(taskId)).thenReturn(Optional.of(existingTask));

            // When & Then
            assertThatThrownBy(() -> service.updateTask(taskId, userId, "New description", null, null))
                .isInstanceOf(TaskBusinessRuleException.class)
                .hasMessageContaining("User can only access their own tasks");
        }
    }

    @Nested
    @DisplayName("Read Task Tests")
    class ReadTaskTests {

        @Test
        @DisplayName("Should get task successfully when user owns it")
        void shouldGetTaskSuccessfully() {
            // Given
            Task task = new Task(taskId, userId, "Test task", Priority.MEDIUM, now);
            when(repository.findById(taskId)).thenReturn(Optional.of(task));

            // When
            Task result = service.getTask(taskId, userId);

            // Then
            assertThat(result).isEqualTo(task);
        }

        @Test
        @DisplayName("Should fail when task not found")
        void shouldFailWhenTaskNotFound() {
            // Given
            when(repository.findById(taskId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.getTask(taskId, userId))
                .isInstanceOf(TaskNotFoundException.class);
        }

        @Test
        @DisplayName("Should list user tasks successfully")
        void shouldListUserTasks() {
            // Given
            Task task1 = new Task(TaskId.generate(), userId, "Task 1", Priority.HIGH, now.minusHours(2));
            Task task2 = new Task(TaskId.generate(), userId, "Task 2", Priority.MEDIUM, now.minusHours(1));
            List<Task> tasks = List.of(task1, task2);

            when(repository.findByUserId(userId, 0, 10)).thenReturn(tasks);

            // When
            List<Task> result = service.listTasks(userId, 0, 10);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getDescription()).isEqualTo("Task 1");
            assertThat(result.get(1).getDescription()).isEqualTo("Task 2");
        }
    }

    @Nested
    @DisplayName("Delete Task Tests")
    class DeleteTaskTests {

        @Test
        @DisplayName("Should delete task successfully when user owns it")
        void shouldDeleteTaskSuccessfully() {
            // Given
            Task task = new Task(taskId, userId, "Test task", Priority.MEDIUM, now);
            when(repository.findById(taskId)).thenReturn(Optional.of(task));

            // When
            service.deleteTask(taskId, userId);

            // Then
            verify(repository).deleteById(taskId);
        }

        @Test
        @DisplayName("Should fail when user tries to delete another user's task")
        void shouldFailWhenDeletingOtherUserTask() {
            // Given
            Task task = new Task(taskId, otherUserId, "Other user task", Priority.MEDIUM, now);
            when(repository.findById(taskId)).thenReturn(Optional.of(task));

            // When & Then
            assertThatThrownBy(() -> service.deleteTask(taskId, userId))
                .isInstanceOf(TaskBusinessRuleException.class)
                .hasMessageContaining("User can only access their own tasks");
        }
    }
}
