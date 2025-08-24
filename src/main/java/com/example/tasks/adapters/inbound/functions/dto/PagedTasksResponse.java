package com.example.tasks.adapters.inbound.functions.dto;

import java.util.List;

public record PagedTasksResponse(
    List<TaskResponse> tasks
) {}
