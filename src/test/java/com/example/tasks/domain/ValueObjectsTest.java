package com.example.tasks.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

class ValueObjectsTest {

    @Nested
    @DisplayName("TaskId Value Object")
    class TaskIdTests {

        @Test
        @DisplayName("Should create TaskId with valid UUID")
        void shouldCreateTaskIdWithValidUuid() {
            // Given
            String validUuid = "123e4567-e89b-12d3-a456-426614174000";

            // When
            TaskId taskId = TaskId.of(validUuid);

            // Then
            assertThat(taskId.value()).isEqualTo(validUuid);
        }

        @Test
        @DisplayName("Should generate valid TaskId")
        void shouldGenerateValidTaskId() {
            // When
            TaskId taskId = TaskId.generate();

            // Then
            assertThat(taskId.value()).isNotNull();
            assertThat(taskId.value()).matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
        }

        @Test
        @DisplayName("Should fail when TaskId is null")
        void shouldFailWhenTaskIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> TaskId.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Task ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should fail when TaskId is empty")
        void shouldFailWhenTaskIdIsEmpty() {
            // When & Then
            assertThatThrownBy(() -> TaskId.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Task ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should fail when TaskId is not valid UUID format")
        void shouldFailWhenTaskIdIsNotValidUuid() {
            // When & Then
            assertThatThrownBy(() -> TaskId.of("invalid-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Task ID must be a valid UUID format");
        }

        @Test
        @DisplayName("Should be equal when values are the same")
        void shouldBeEqualWhenValuesAreSame() {
            // Given
            String uuid = "123e4567-e89b-12d3-a456-426614174000";
            TaskId taskId1 = TaskId.of(uuid);
            TaskId taskId2 = TaskId.of(uuid);

            // When & Then
            assertThat(taskId1).isEqualTo(taskId2);
            assertThat(taskId1.hashCode()).isEqualTo(taskId2.hashCode());
        }
    }

    @Nested
    @DisplayName("UserId Value Object")
    class UserIdTests {

        @Test
        @DisplayName("Should create UserId with valid value")
        void shouldCreateUserIdWithValidValue() {
            // Given
            String validUserId = "user123";

            // When
            UserId userId = UserId.of(validUserId);

            // Then
            assertThat(userId.value()).isEqualTo(validUserId);
        }

        @Test
        @DisplayName("Should trim whitespace from UserId")
        void shouldTrimWhitespaceFromUserId() {
            // Given
            String userIdWithSpaces = "  user123  ";

            // When
            UserId userId = UserId.of(userIdWithSpaces);

            // Then
            assertThat(userId.value()).isEqualTo("user123");
        }

        @Test
        @DisplayName("Should fail when UserId is null")
        void shouldFailWhenUserIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> UserId.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should fail when UserId is empty after trimming")
        void shouldFailWhenUserIdIsEmptyAfterTrimming() {
            // When & Then
            assertThatThrownBy(() -> UserId.of("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should fail when UserId exceeds 100 characters")
        void shouldFailWhenUserIdTooLong() {
            // Given
            String longUserId = "a".repeat(101);

            // When & Then
            assertThatThrownBy(() -> UserId.of(longUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID cannot exceed 100 characters");
        }

        @Test
        @DisplayName("Should accept UserId with exactly 100 characters")
        void shouldAcceptUserIdWithExactly100Characters() {
            // Given
            String maxLengthUserId = "a".repeat(100);

            // When
            UserId userId = UserId.of(maxLengthUserId);

            // Then
            assertThat(userId.value()).hasSize(100);
        }

        @Test
        @DisplayName("Should be equal when values are the same")
        void shouldBeEqualWhenValuesAreSame() {
            // Given
            String userIdValue = "user123";
            UserId userId1 = UserId.of(userIdValue);
            UserId userId2 = UserId.of(userIdValue);

            // When & Then
            assertThat(userId1).isEqualTo(userId2);
            assertThat(userId1.hashCode()).isEqualTo(userId2.hashCode());
        }
    }

    @Nested
    @DisplayName("Priority Enum")
    class PriorityTests {

        @Test
        @DisplayName("Should have all expected priority values")
        void shouldHaveAllExpectedPriorityValues() {
            // When & Then
            assertThat(Priority.values()).containsExactly(Priority.LOW, Priority.MEDIUM, Priority.HIGH);
        }

        @Test
        @DisplayName("Should convert from string correctly")
        void shouldConvertFromStringCorrectly() {
            // When & Then
            assertThat(Priority.valueOf("LOW")).isEqualTo(Priority.LOW);
            assertThat(Priority.valueOf("MEDIUM")).isEqualTo(Priority.MEDIUM);
            assertThat(Priority.valueOf("HIGH")).isEqualTo(Priority.HIGH);
        }
    }

    @Nested
    @DisplayName("Status Enum")
    class StatusTests {

        @Test
        @DisplayName("Should have all expected status values")
        void shouldHaveAllExpectedStatusValues() {
            // When & Then
            assertThat(Status.values()).containsExactly(Status.OPEN, Status.COMPLETED);
        }

        @Test
        @DisplayName("Should convert from string correctly")
        void shouldConvertFromStringCorrectly() {
            // When & Then
            assertThat(Status.valueOf("OPEN")).isEqualTo(Status.OPEN);
            assertThat(Status.valueOf("COMPLETED")).isEqualTo(Status.COMPLETED);
        }
    }
}
