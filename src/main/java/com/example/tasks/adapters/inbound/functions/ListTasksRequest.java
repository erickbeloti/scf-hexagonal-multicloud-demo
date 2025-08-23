package com.example.tasks.adapters.inbound.functions;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ListTasksRequest(
    @NotBlank(message = "User ID is required")
    String userId,
    
    @Min(value = 0, message = "Page must be 0 or greater")
    int page,
    
    @Min(value = 1, message = "Size must be 1 or greater")
    int size
) {}