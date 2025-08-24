package com.example.tasks.application.port.inbound;

import com.example.tasks.domain.Priority;
import com.example.tasks.domain.Task;

public interface CreateTaskUseCase {
    Task createTask(String userId, String description, Priority priority);
}
