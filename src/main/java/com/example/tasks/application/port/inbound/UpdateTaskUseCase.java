package com.example.tasks.application.port.inbound;

import com.example.tasks.domain.*;

public interface UpdateTaskUseCase {
    Task updateTask(TaskId id, UserId userId, String description, Priority priority, Status status);
}
