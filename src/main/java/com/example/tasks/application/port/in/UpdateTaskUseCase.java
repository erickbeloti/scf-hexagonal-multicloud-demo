package com.example.tasks.application.port.in;

import com.example.tasks.domain.model.Task;

public interface UpdateTaskUseCase {
    Task updateTask(Task task);
}
