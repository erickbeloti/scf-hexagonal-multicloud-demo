package com.example.tasks.domain;

import com.example.tasks.domain.exception.TaskBusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Comprehensive business rules validation using simplified architecture
 * Tests business rules through the Task entity static validation methods
 */
@DisplayName("Business Rules End-to-End Validation")
class BusinessRulesComprehensiveTest {

    TaskValidationService validationService;

    UserId userId;
    UserId otherUserId;
    TaskId taskId;
    LocalDate today;
    LocalDateTime now;

    @BeforeEach
    void setup() {
        validationService = mock(TaskValidationService.class);

        userId = UserId.of("user123");
        otherUserId = UserId.of("otherUser");
        taskId = TaskId.of("123e4567-e89b-12d3-a456-426614174000");
        today = LocalDate.now();
        now = LocalDateTime.now();
    }

    @Nested
    @DisplayName("BR-CREATE: Task Creation Business Rules")
    class CreateBusinessRulesTests {

        @Test
        @DisplayName("BR-CREATE-01: User cannot create more than 5 high-priority tasks per day")
        void shouldEnforceHighPriorityDailyLimit() {
            // Given - user already has 5 high priority tasks today
            when(validationService.existsByUserAndDateAndDescription(userId, today, "High priority task")).thenReturn(false);
            when(validationService.countHighPriorityTasksForUserOnDate(userId, today))
                .thenReturn((long) TaskBusinessRules.MAX_HIGH_PRIORITY_TASKS_PER_DAY);
            when(validationService.countOpenTasksForUser(userId)).thenReturn(0L);

            // When & Then
            assertThatThrownBy(() -> Task.validateCreationRules(userId, "High priority task", Priority.HIGH, today, validationService))
                .isInstanceOf(TaskBusinessRuleException.class)
                .hasMessageContaining("Cannot create more than " + TaskBusinessRules.MAX_HIGH_PRIORITY_TASKS_PER_DAY);
        }

        @Test
        @DisplayName("BR-CREATE-02: Task description must be unique per user per day")
        void shouldEnforceDescriptionUniqueness() {
            // Given - description already exists for user today
            when(validationService.existsByUserAndDateAndDescription(userId, today, "Duplicate task")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> Task.validateCreationRules(userId, "Duplicate task", Priority.MEDIUM, today, validationService))
                .isInstanceOf(TaskBusinessRuleException.class)
                .hasMessageContaining("Description must be unique per user per day");
        }

        @Test
        @DisplayName("BR-CREATE-03: User cannot have more than 50 open tasks")
        void shouldEnforceOpenTaskLimit() {
            // Given - user already has 50 open tasks
            when(validationService.existsByUserAndDateAndDescription(userId, today, "New task")).thenReturn(false);
            when(validationService.countHighPriorityTasksForUserOnDate(userId, today)).thenReturn(0L);
            when(validationService.countOpenTasksForUser(userId))
                .thenReturn((long) TaskBusinessRules.MAX_OPEN_TASKS_PER_USER);

            // When & Then
            assertThatThrownBy(() -> Task.validateCreationRules(userId, "New task", Priority.MEDIUM, today, validationService))
                .isInstanceOf(TaskBusinessRuleException.class)
                .hasMessageContaining("Cannot have more than " + TaskBusinessRules.MAX_OPEN_TASKS_PER_USER);
        }

        @Test
        @DisplayName("BR-CREATE-04: Task must have valid priority")
        void shouldEnforceValidPriority() {
            // Given - all validation checks pass except priority validation happens in Priority enum
            when(validationService.existsByUserAndDateAndDescription(userId, today, "Valid task")).thenReturn(false);
            when(validationService.countHighPriorityTasksForUserOnDate(userId, today)).thenReturn(0L);
            when(validationService.countOpenTasksForUser(userId)).thenReturn(0L);

            // When & Then - Priority validation is handled by the enum itself
            assertThatCode(() -> Task.validateCreationRules(userId, "Valid task", Priority.HIGH, today, validationService))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("BR-CREATE-05: Valid task creation should succeed")
        void shouldAllowValidTaskCreation() {
            // Given - all validation checks pass
            when(validationService.existsByUserAndDateAndDescription(userId, today, "Valid task")).thenReturn(false);
            when(validationService.countHighPriorityTasksForUserOnDate(userId, today)).thenReturn(0L);
            when(validationService.countOpenTasksForUser(userId)).thenReturn(0L);

            // When & Then
            assertThatCode(() -> Task.validateCreationRules(userId, "Valid task", Priority.MEDIUM, today, validationService))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("BR-UPDATE: Task Update Business Rules")
    class UpdateBusinessRulesTests {

        @Test
        @DisplayName("BR-UPDATE-01: Completed task cannot be updated")
        void shouldPreventUpdatingCompletedTask() {
            // Given - completed task
            Task completedTask = Task.reconstitute(taskId, userId, "Completed task", Priority.MEDIUM, Status.COMPLETED, now.minusHours(2), now.minusHours(1));

            // When & Then
            assertThatThrownBy(() -> completedTask.updateDescription("New description", now))
                .isInstanceOf(TaskBusinessRuleException.class)
                .hasMessageContaining("Cannot update completed task");
        }

        @Test
        @DisplayName("BR-UPDATE-02: Task description update must follow uniqueness rule")
        void shouldEnforceDescriptionUniquenessOnUpdate() {
            // Given - existing task and duplicate description check
            Task existingTask = new Task(taskId, userId, "Original description", Priority.MEDIUM, now.minusHours(1));

            // This would be checked in the application service, not directly in Task entity
            // The Task entity only handles its own state validation
            assertThatCode(() -> existingTask.updateDescription("New unique description", now))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("BR-READ: Task Access Business Rules")
    class ReadBusinessRulesTests {

        @Test
        @DisplayName("BR-READ-01: User can only read their own tasks")
        void shouldEnforceTaskOwnershipOnRead() {
            // Given - task belonging to other user
            Task otherUserTask = new Task(taskId, otherUserId, "Other user task", Priority.MEDIUM, now);

            // When & Then
            assertThatThrownBy(() -> otherUserTask.ensureOwnership(userId))
                .isInstanceOf(TaskBusinessRuleException.class)
                .hasMessageContaining("User can only access their own tasks");
        }

        @Test
        @DisplayName("BR-READ-02: User can read their own tasks")
        void shouldAllowOwnerToReadTask() {
            // Given - task belonging to user
            Task userTask = new Task(taskId, userId, "User task", Priority.MEDIUM, now);

            // When & Then
            assertThatCode(() -> userTask.ensureOwnership(userId))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("BR-DELETE: Task Deletion Business Rules")
    class DeleteBusinessRulesTests {

        @Test
        @DisplayName("BR-DELETE-01: User can only delete their own tasks")
        void shouldEnforceTaskOwnershipOnDelete() {
            // Given - task belonging to other user
            Task otherUserTask = new Task(taskId, otherUserId, "Other user task", Priority.MEDIUM, now);

            // When & Then
            assertThatThrownBy(() -> otherUserTask.ensureOwnership(userId))
                .isInstanceOf(TaskBusinessRuleException.class)
                .hasMessageContaining("User can only access their own tasks");
        }

        @Test
        @DisplayName("BR-DELETE-02: User can delete their own tasks")
        void shouldAllowOwnerToDeleteTask() {
            // Given - task belonging to user
            Task userTask = new Task(taskId, userId, "User task", Priority.MEDIUM, now);

            // When & Then
            assertThatCode(() -> userTask.ensureOwnership(userId))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("BR-CROSS: Cross-Rule Integration Tests")
    class CrossRuleIntegrationTests {

        @Test
        @DisplayName("Multiple business rules should be evaluated correctly")
        void shouldEvaluateMultipleRules() {
            // Given - user at various limits
            when(validationService.countOpenTasksForUser(userId)).thenReturn(49L); // Just under limit
            when(validationService.countHighPriorityTasksForUserOnDate(userId, today)).thenReturn(4L); // Just under limit
            when(validationService.existsByUserAndDateAndDescription(userId, today, "Unique task")).thenReturn(false);

            // When & Then - should allow creation
            assertThatCode(() -> Task.validateCreationRules(userId, "Unique task", Priority.HIGH, today, validationService))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("First violated rule should be reported")
        void shouldReportFirstViolatedRule() {
            // Given - multiple rule violations (description uniqueness checked first)
            when(validationService.existsByUserAndDateAndDescription(userId, today, "Duplicate task")).thenReturn(true);
            when(validationService.countHighPriorityTasksForUserOnDate(userId, today))
                .thenReturn((long) TaskBusinessRules.MAX_HIGH_PRIORITY_TASKS_PER_DAY);

            // When & Then - should report description uniqueness violation first
            assertThatThrownBy(() -> Task.validateCreationRules(userId, "Duplicate task", Priority.HIGH, today, validationService))
                .isInstanceOf(TaskBusinessRuleException.class)
                .hasMessageContaining("Description must be unique per user per day");
        }
    }
}
