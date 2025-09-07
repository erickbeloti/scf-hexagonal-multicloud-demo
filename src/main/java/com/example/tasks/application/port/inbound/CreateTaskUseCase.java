package com.example.tasks.application.port.inbound;

import com.example.tasks.domain.*;

public interface CreateTaskUseCase {
    Task createTask(UserId userId, String description, Priority priority);
}
