package com.example.tasks.application.port.inbound;

public interface DeleteTaskUseCase {
    void deleteTask(String taskId, String userId);
}