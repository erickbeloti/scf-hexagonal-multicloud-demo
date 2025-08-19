package com.example.tasks.application.port.in;

import com.example.tasks.domain.model.Task;

public interface CreateTaskUseCase {
    Task createTask(Task task);
}
