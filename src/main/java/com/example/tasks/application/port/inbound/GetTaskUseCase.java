package com.example.tasks.application.port.inbound;

import com.example.tasks.domain.*;

public interface GetTaskUseCase {
    Task getTask(TaskId id, UserId userId);
}
