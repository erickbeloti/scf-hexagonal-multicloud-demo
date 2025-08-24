package com.example.tasks.adapters.inbound.functions;

import com.example.tasks.adapters.inbound.functions.dto.*;
import com.example.tasks.application.service.TaskService;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskFunctions {

    private final TaskService service;

    public TaskFunctions(TaskService service) {
        this.service = service;
    }

    @Bean
    public Function<CreateTaskRequest, TaskResponse> createTask() {
        return request -> TaskDtoMapper.toResponse(
            service.createTask(request.userId(), request.description(), request.priority()));
    }

    @Bean
    public Function<UpdateTaskRequest, TaskResponse> updateTask() {
        return request -> TaskDtoMapper.toResponse(
            service.updateTask(request.id(), request.userId(), request.description(), request.priority(), request.status()));
    }

    @Bean
    public Function<GetTaskRequest, TaskResponse> getTaskById() {
        return request -> TaskDtoMapper.toResponse(
            service.getTask(request.id(), request.userId()));
    }

    @Bean
    public Function<ListTasksRequest, PagedTasksResponse> listTasksByUser() {
        return request -> TaskDtoMapper.toPagedResponse(
            service.listTasks(request.userId(), request.page(), request.size()));
    }

    @Bean
    public Function<DeleteTaskRequest, DeleteTaskResponse> deleteTask() {
        return request -> {
            service.deleteTask(request.id(), request.userId());
            return new DeleteTaskResponse(request.id());
        };
    }
}
