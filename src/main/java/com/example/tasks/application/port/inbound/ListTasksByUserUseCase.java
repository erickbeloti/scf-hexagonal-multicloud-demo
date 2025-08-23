package com.example.tasks.application.port.inbound;

import com.example.tasks.domain.model.Task;
import java.util.List;

public interface ListTasksByUserUseCase {
    List<Task> list(String userId, int page, int size);
}

