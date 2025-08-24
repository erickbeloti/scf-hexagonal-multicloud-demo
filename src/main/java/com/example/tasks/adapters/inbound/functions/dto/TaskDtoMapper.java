package com.example.tasks.adapters.inbound.functions.dto;

import com.example.tasks.domain.Task;
import java.util.List;

public final class TaskDtoMapper {
    private TaskDtoMapper() {}

    public static TaskResponse toResponse(Task task) {
        return new TaskResponse(
            task.id(),
            task.userId(),
            task.description(),
            task.priority(),
            task.status(),
            task.createdAt(),
            task.updatedAt()
        );
    }

    public static PagedTasksResponse toPagedResponse(List<Task> tasks) {
        return new PagedTasksResponse(tasks.stream().map(TaskDtoMapper::toResponse).toList());
    }
}
