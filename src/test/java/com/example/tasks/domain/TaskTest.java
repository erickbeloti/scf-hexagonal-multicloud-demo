package com.example.tasks.domain;

import static org.assertj.core.api.Assertions.*;

import com.example.tasks.domain.exception.TaskBusinessRuleException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

@DisplayName("Task Entity")
class TaskTest {

    TaskId taskId;
    UserId userId;
    LocalDateTime now;
    Task task;

    @BeforeEach
    void setup() {
        taskId = TaskId.of("123e4567-e89b-12d3-a456-426614174000");
        userId = UserId.of("user123");
        now = LocalDateTime.now();
        task = new Task(taskId, userId, "Test task", Priority.MEDIUM, now);
    }

    @Nested
    @DisplayName("Task Creation and Validation")
    class TaskCreationTests {

        @Test
        @DisplayName("Should create task with valid data")
        void shouldCreateTaskWithValidData() {
            // When
            Task newTask = new Task(taskId, userId, "Valid task", Priority.HIGH, now);

            // Then
            assertThat(newTask.getId()).isEqualTo(taskId);
            assertThat(newTask.getUserId()).isEqualTo(userId);
            assertThat(newTask.getDescription()).isEqualTo("Valid task");
            assertThat(newTask.getPriority()).isEqualTo(Priority.HIGH);
            assertThat(newTask.getStatus()).isEqualTo(Status.OPEN);
            assertThat(newTask.getCreatedAt()).isEqualTo(now);
            assertThat(newTask.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should fail with null task ID")
        void shouldFailWithNullTaskId() {
            assertThatThrownBy(() ->
                new Task(null, userId, "Valid task", Priority.HIGH, now))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Task ID cannot be null");
        }

        @Test
        @DisplayName("Should fail with null user ID")
        void shouldFailWithNullUserId() {
            assertThatThrownBy(() ->
                new Task(taskId, null, "Valid task", Priority.HIGH, now))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("User ID cannot be null");
        }

        @Test
        @DisplayName("Should fail with invalid description")
        void shouldFailWithInvalidDescription() {
            assertThatThrownBy(() ->
                new Task(taskId, userId, null, Priority.HIGH, now))
                .isInstanceOf(TaskBusinessRuleException.class)
                .hasMessageContaining("Description cannot be empty");

            assertThatThrownBy(() ->
                new Task(taskId, userId, "", Priority.HIGH, now))
                .isInstanceOf(TaskBusinessRuleException.class)
                .hasMessageContaining("Description cannot be empty");

            assertThatThrownBy(() ->
                new Task(taskId, userId, "   ", Priority.HIGH, now))
                .isInstanceOf(TaskBusinessRuleException.class)
                .hasMessageContaining("Description cannot be empty");
        }

        @Test
        @DisplayName("Should fail with description too long")
        void shouldFailWithDescriptionTooLong() {
            String longDescription = "a".repeat(501);

            assertThatThrownBy(() ->
                new Task(taskId, userId, longDescription, Priority.HIGH, now))
                .isInstanceOf(TaskBusinessRuleException.class)
                .hasMessageContaining("Description cannot exceed");
        }

        @Test
        @DisplayName("Should trim description")
        void shouldTrimDescription() {
            Task trimmedTask = new Task(taskId, userId, "  Valid task  ", Priority.HIGH, now);
            assertThat(trimmedTask.getDescription()).isEqualTo("Valid task");
        }

        @Test
        @DisplayName("Should fail with null priority")
        void shouldFailWithNullPriority() {
            assertThatThrownBy(() ->
                new Task(taskId, userId, "Valid task", null, now))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Priority cannot be null");
        }
    }

    @Nested
    @DisplayName("Task State Changes")
    class TaskStateChangeTests {

        @Test
        @DisplayName("Should update description successfully")
        void shouldUpdateDescriptionSuccessfully() {
            // Given
            LocalDateTime updateTime = now.plusMinutes(10);
            String newDescription = "Updated description";

            // When
            Task updatedTask = task.updateDescription(newDescription, updateTime);

            // Then
            assertThat(updatedTask.getDescription()).isEqualTo(newDescription);
            assertThat(updatedTask.getUpdatedAt()).isEqualTo(updateTime);
        }

        @Test
        @DisplayName("Should change priority successfully")
        void shouldChangePrioritySuccessfully() {
            // Given
            LocalDateTime updateTime = now.plusMinutes(10);

            // When
            Task updatedTask = task.changePriority(Priority.HIGH, updateTime);

            // Then
            assertThat(updatedTask.getPriority()).isEqualTo(Priority.HIGH);
            assertThat(updatedTask.getUpdatedAt()).isEqualTo(updateTime);
        }

        @Test
        @DisplayName("Should complete task successfully")
        void shouldCompleteTaskSuccessfully() {
            // Given
            LocalDateTime completionTime = now.plusMinutes(30);

            // When
            Task completedTask = task.complete(completionTime);

            // Then
            assertThat(completedTask.getStatus()).isEqualTo(Status.COMPLETED);
            assertThat(completedTask.getUpdatedAt()).isEqualTo(completionTime);
            assertThat(completedTask.isOpen()).isFalse();
        }

        @Test
        @DisplayName("Should fail to complete already completed task")
        void shouldFailToCompleteAlreadyCompletedTask() {
            // Given
            Task completedTask = task.complete(now.plusMinutes(30));

            // When & Then
            assertThatThrownBy(() -> completedTask.complete(now.plusMinutes(60)))
                .isInstanceOf(TaskBusinessRuleException.class)
                .hasMessageContaining("Task is already completed");
        }

        @Test
        @DisplayName("Should prevent updates to completed task")
        void shouldPreventUpdatesToCompletedTask() {
            // Given
            Task completedTask = task.complete(now.plusMinutes(30));

            // When & Then
            assertThatThrownBy(() ->
                completedTask.updateDescription("New description", now.plusMinutes(60)))
                .isInstanceOf(TaskBusinessRuleException.class)
                .hasMessageContaining("Cannot update completed task");

            assertThatThrownBy(() ->
                completedTask.changePriority(Priority.HIGH, now.plusMinutes(60)))
                .isInstanceOf(TaskBusinessRuleException.class)
                .hasMessageContaining("Cannot update completed task");
        }
    }

    @Nested
    @DisplayName("Task Query Methods")
    class TaskQueryTests {

        @Test
        @DisplayName("Should check task ownership correctly")
        void shouldCheckTaskOwnershipCorrectly() {
            UserId otherUser = UserId.of("otherUser");

            assertThat(task.belongsTo(userId)).isTrue();
            assertThat(task.belongsTo(otherUser)).isFalse();
        }

        @Test
        @DisplayName("Should check if task is high priority")
        void shouldCheckIfTaskIsHighPriority() {
            Task lowPriorityTask = new Task(taskId, userId, "Low task", Priority.LOW, now);
            Task mediumPriorityTask = new Task(taskId, userId, "Medium task", Priority.MEDIUM, now);
            Task highPriorityTask = new Task(taskId, userId, "High task", Priority.HIGH, now);

            assertThat(lowPriorityTask.isHighPriority()).isFalse();
            assertThat(mediumPriorityTask.isHighPriority()).isFalse();
            assertThat(highPriorityTask.isHighPriority()).isTrue();
        }

        @Test
        @DisplayName("Should check if task was created on specific date")
        void shouldCheckIfTaskWasCreatedOnSpecificDate() {
            assertThat(task.wasCreatedOn(now.toLocalDate())).isTrue();
            assertThat(task.wasCreatedOn(now.toLocalDate().plusDays(1))).isFalse();
        }
    }
}
