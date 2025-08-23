package com.example.tasks.domain.policy;

import com.example.tasks.domain.error.DomainException;
import com.example.tasks.domain.model.Priority;

public class TaskCreationPolicy {
    public void validate(Priority priority,
                         long numHighPriorityToday,
                         boolean isDescriptionUnique,
                         long numOpenTasksForUser) {
        if (priority == Priority.HIGH && numHighPriorityToday >= 5) {
            throw new DomainException("RULE_HIGH_TASKS_LIMIT", "A user cannot create more than 5 HIGH-priority tasks per day");
        }
        if (!isDescriptionUnique) {
            throw new DomainException("RULE_DESCRIPTION_UNIQUE", "Description must be unique per user per day");
        }
        if (numOpenTasksForUser >= 50) {
            throw new DomainException("RULE_OPEN_TASKS_LIMIT", "A user cannot have more than 50 open tasks");
        }
    }
}

