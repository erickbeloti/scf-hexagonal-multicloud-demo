package com.example.tasks.application.port.inbound;

import com.example.tasks.domain.*;

public interface UpdateTaskUseCase {
    Task updateTask(String id, String userId, String description, Priority priority, Status status);
}
