package com.example.tasks.adapters.inbound.functions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateTaskRequest(
    @NotBlank(message = "User ID is required")
    String userId,
    
    @NotBlank(message = "Description is required")
    String description,
    
    @NotBlank(message = "Priority is required")
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH)$", message = "Priority must be LOW, MEDIUM, or HIGH")
    String priority
) {}