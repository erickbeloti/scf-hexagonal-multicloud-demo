package com.example.tasks.adapters.inbound.functions.dto;

import com.example.tasks.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTaskRequest(
    @NotBlank String userId,
    @NotBlank String description,
    @NotNull Priority priority
) {}
