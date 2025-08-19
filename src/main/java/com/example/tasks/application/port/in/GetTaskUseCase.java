package com.example.tasks.application.port.in;

import com.example.tasks.domain.model.Task;

import java.util.Optional;

public interface GetTaskUseCase {
    Optional<Task> getTask(String id);
}
