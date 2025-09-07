package com.example.tasks.adapters.inbound.functions.dto;

import com.example.tasks.domain.Task;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class TaskDtoMapper {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private TaskDtoMapper() {}

    public static TaskResponse toResponse(Task task) {
        return new TaskResponse(
            task.getId().value(),
            task.getUserId().value(),
            task.getDescription(),
            task.getPriority(),
            task.getStatus(),
            task.getCreatedAt().format(FORMATTER),
            task.getUpdatedAt().format(FORMATTER)
        );
    }

    public static PagedTasksResponse toPagedResponse(List<Task> tasks) {
        return new PagedTasksResponse(tasks.stream().map(TaskDtoMapper::toResponse).toList());
    }
}
