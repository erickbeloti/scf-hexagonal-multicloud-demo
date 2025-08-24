package com.example.tasks.adapters.inbound.functions.dto;

import com.example.tasks.domain.Priority;
import com.example.tasks.domain.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskRequest(
    @NotBlank String id,
    @NotBlank String userId,
    @NotBlank String description,
    @NotNull Priority priority,
    Status status
) {}
