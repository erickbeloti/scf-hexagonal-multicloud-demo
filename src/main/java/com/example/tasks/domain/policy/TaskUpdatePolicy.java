package com.example.tasks.domain.policy;

import com.example.tasks.domain.error.DomainException;
import com.example.tasks.domain.model.Task;

public class TaskUpdatePolicy {
    public void validateUpdatable(Task existingTask) {
        if (existingTask.isCompleted()) {
            throw new DomainException("RULE_COMPLETED_IMMUTABLE", "Completed tasks cannot be updated");
        }
    }
}

