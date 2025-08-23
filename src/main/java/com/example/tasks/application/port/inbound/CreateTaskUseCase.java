package com.example.tasks.application.port.inbound;

import com.example.tasks.domain.model.Task;

public interface CreateTaskUseCase {
    Task create(Task draft);
}

