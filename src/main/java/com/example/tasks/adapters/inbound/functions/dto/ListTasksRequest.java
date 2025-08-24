package com.example.tasks.adapters.inbound.functions.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ListTasksRequest(
    @NotBlank String userId,
    @Min(0) int page,
    @Min(1) int size
) {}
