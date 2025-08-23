package com.example.tasks.application.port.inbound;

import com.example.tasks.domain.Task;

public interface GetTaskUseCase {
    Task getTask(String taskId, String userId);
}