package com.example.tasks.domain;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

class PriorityTest {

    @Nested
    @DisplayName("Priority Validation")
    class PriorityValidationTests {

        @Test
        @DisplayName("Should validate HIGH priority correctly")
        void shouldValidateHighPriority() {
            assertThat(Priority.HIGH.isHighPriority()).isTrue();
            assertThat(Priority.MEDIUM.isHighPriority()).isFalse();
            assertThat(Priority.LOW.isHighPriority()).isFalse();
        }

        @Test
        @DisplayName("Should create priority from valid string")
        void shouldCreatePriorityFromValidString() {
            assertThat(Priority.fromString("HIGH")).isEqualTo(Priority.HIGH);
            assertThat(Priority.fromString("high")).isEqualTo(Priority.HIGH);
            assertThat(Priority.fromString(" MEDIUM ")).isEqualTo(Priority.MEDIUM);
            assertThat(Priority.fromString("low")).isEqualTo(Priority.LOW);
        }

        @Test
        @DisplayName("Should fail for invalid priority string")
        void shouldFailForInvalidPriorityString() {
            assertThatThrownBy(() -> Priority.fromString("INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid priority: INVALID");

            assertThatThrownBy(() -> Priority.fromString(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Priority cannot be null or empty");

            assertThatThrownBy(() -> Priority.fromString(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Priority cannot be null or empty");
        }

        @Test
        @DisplayName("Should validate priority string correctly")
        void shouldValidatePriorityString() {
            assertThat(Priority.isValid("HIGH")).isTrue();
            assertThat(Priority.isValid("medium")).isTrue();
            assertThat(Priority.isValid(" LOW ")).isTrue();

            assertThat(Priority.isValid("INVALID")).isFalse();
            assertThat(Priority.isValid(null)).isFalse();
            assertThat(Priority.isValid("")).isFalse();
        }

        @Test
        @DisplayName("Should have correct display names")
        void shouldHaveCorrectDisplayNames() {
            assertThat(Priority.HIGH.getDisplayName()).isEqualTo("High");
            assertThat(Priority.MEDIUM.getDisplayName()).isEqualTo("Medium");
            assertThat(Priority.LOW.getDisplayName()).isEqualTo("Low");
        }
    }
}
