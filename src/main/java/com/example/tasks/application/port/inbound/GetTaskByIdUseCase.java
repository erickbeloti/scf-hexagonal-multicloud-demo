package com.example.tasks.application.port.inbound;

import com.example.tasks.domain.model.Task;
import java.util.Optional;

public interface GetTaskByIdUseCase {
    Optional<Task> get(String id, String userId);
}

