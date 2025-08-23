package com.example.tasks.application.port.inbound;

public interface DeleteTaskUseCase {
    boolean delete(String id, String userId);
}

