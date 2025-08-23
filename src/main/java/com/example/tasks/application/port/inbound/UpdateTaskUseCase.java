package com.example.tasks.application.port.inbound;

import com.example.tasks.domain.Task;

public interface UpdateTaskUseCase {
    Task updateTask(String taskId, String userId, String description, String priority, String status);
}