package com.example.tasks.adapters.inbound.functions.dto;

import com.example.tasks.domain.Priority;
import com.example.tasks.domain.Status;

public record TaskResponse(
    String id,
    String userId,
    String description,
    Priority priority,
    Status status,
    String createdAt,
    String updatedAt
) {}
