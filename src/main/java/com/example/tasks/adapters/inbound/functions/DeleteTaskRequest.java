package com.example.tasks.adapters.inbound.functions;

import jakarta.validation.constraints.NotBlank;

public record DeleteTaskRequest(
    @NotBlank(message = "Task ID is required")
    String taskId,
    
    @NotBlank(message = "User ID is required")
    String userId
) {}