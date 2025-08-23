package com.example.tasks.adapters.inbound.http;

import com.example.tasks.adapters.inbound.http.dto.TaskDtos;
import com.example.tasks.domain.model.Task;

public class TaskMappers {
    public static TaskDtos.TaskResponse toResponse(Task t) {
        return new TaskDtos.TaskResponse(
                t.id(), t.userId(), t.description(), t.priority(), t.status(), t.createdAt(), t.updatedAt()
        );
    }
}

