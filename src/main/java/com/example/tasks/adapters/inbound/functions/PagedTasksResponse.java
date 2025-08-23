package com.example.tasks.adapters.inbound.functions;

import java.util.List;

public record PagedTasksResponse(
    List<TaskResponse> tasks,
    int page,
    int size
) {}