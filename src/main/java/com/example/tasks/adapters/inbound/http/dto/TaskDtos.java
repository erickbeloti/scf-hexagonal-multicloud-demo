package com.example.tasks.adapters.inbound.http.dto;

import com.example.tasks.domain.model.Priority;
import com.example.tasks.domain.model.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public class TaskDtos {
    public record CreateTaskRequest(
            @NotBlank String description,
            @NotNull Priority priority
    ) {}

    public record UpdateTaskRequest(
            @Size(min = 1) String description,
            Priority priority,
            Status status
    ) {}

    public record TaskResponse(
            String id,
            String userId,
            String description,
            Priority priority,
            Status status,
            Instant createdAt,
            Instant updatedAt
    ) {}
}

