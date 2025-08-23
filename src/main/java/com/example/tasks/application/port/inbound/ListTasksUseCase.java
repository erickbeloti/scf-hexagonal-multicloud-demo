package com.example.tasks.application.port.inbound;

import java.util.List;

public interface ListTasksUseCase {
    List<Task> listTasks(String userId, int page, int size);
}