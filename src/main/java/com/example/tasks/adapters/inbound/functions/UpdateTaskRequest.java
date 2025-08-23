package com.example.tasks.adapters.inbound.functions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateTaskRequest(
    @NotBlank(message = "Task ID is required")
    String taskId,
    
    @NotBlank(message = "User ID is required")
    String userId,
    
    @NotBlank(message = "Description is required")
    String description,
    
    @NotBlank(message = "Priority is required")
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH)$", message = "Priority must be LOW, MEDIUM, or HIGH")
    String priority,
    
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(PENDING|IN_PROGRESS|COMPLETED)$", message = "Status must be PENDING, IN_PROGRESS, or COMPLETED")
    String status
) {}