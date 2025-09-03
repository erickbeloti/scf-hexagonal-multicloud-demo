package com.example.tasks.application.port.inbound;

import com.example.tasks.domain.*;

public interface DeleteTaskUseCase {
    void deleteTask(TaskId id, UserId userId);
}
