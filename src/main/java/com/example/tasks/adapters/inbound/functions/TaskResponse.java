package com.example.tasks.adapters.inbound.functions;

import com.example.tasks.domain.Task;
import com.example.tasks.domain.Priority;
import com.example.tasks.domain.Status;

import java.time.LocalDateTime;

public record TaskResponse(
    String id,
    String userId,
    String description,
    String priority,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    
    public static TaskResponse from(Task task) {
        return new TaskResponse(
            task.id(),
            task.userId(),
            task.description(),
            task.priority().name(),
            task.status().name(),
            task.createdAt(),
            task.updatedAt()
        );
    }
}