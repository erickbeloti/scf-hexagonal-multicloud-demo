package com.example.tasks.domain.policy;

import com.example.tasks.domain.error.DomainException;
import com.example.tasks.domain.model.Priority;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

public class TaskCreationPolicyTest {
    private final TaskCreationPolicy policy = new TaskCreationPolicy();

    @Test
    void allows_when_within_limits() {
        assertThatCode(() -> policy.validate(Priority.MEDIUM, 0, true, 10)).doesNotThrowAnyException();
    }

    @Test
    void blocks_more_than_5_high_per_day() {
        assertThatThrownBy(() -> policy.validate(Priority.HIGH, 5, true, 10))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("HIGH-priority tasks");
    }

    @Test
    void blocks_non_unique_description() {
        assertThatThrownBy(() -> policy.validate(Priority.LOW, 0, false, 10))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Description must be unique");
    }

    @Test
    void blocks_more_than_50_open() {
        assertThatThrownBy(() -> policy.validate(Priority.LOW, 0, true, 50))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("more than 50 open");
    }
}

