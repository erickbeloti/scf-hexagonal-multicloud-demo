package com.example.tasks.adapters.inbound.functions.dto;

import com.example.tasks.domain.Priority;
import com.example.tasks.domain.Status;
import java.time.LocalDateTime;

public record TaskResponse(
    String id,
    String userId,
    String description,
    Priority priority,
    Status status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
