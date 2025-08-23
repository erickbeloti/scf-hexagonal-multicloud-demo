package com.example.tasks.application.port.inbound;

import com.example.tasks.domain.model.Task;

public interface UpdateTaskUseCase {
    Task update(String id, String userId, Task update);
}

