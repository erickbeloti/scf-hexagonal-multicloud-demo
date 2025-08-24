package com.example.tasks.adapters.inbound.functions.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteTaskRequest(
    @NotBlank String id,
    @NotBlank String userId
) {}
