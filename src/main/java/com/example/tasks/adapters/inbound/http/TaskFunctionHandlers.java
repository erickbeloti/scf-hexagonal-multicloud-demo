package com.example.tasks.adapters.inbound.http;

import com.example.tasks.adapters.inbound.http.dto.TaskDtos.CreateTaskRequest;
import com.example.tasks.adapters.inbound.http.dto.TaskDtos.UpdateTaskRequest;
import com.example.tasks.adapters.inbound.http.dto.TaskDtos.TaskResponse;
import com.example.tasks.application.port.inbound.CreateTaskUseCase;
import com.example.tasks.application.port.inbound.DeleteTaskUseCase;
import com.example.tasks.application.port.inbound.GetTaskByIdUseCase;
import com.example.tasks.application.port.inbound.ListTasksByUserUseCase;
import com.example.tasks.application.port.inbound.UpdateTaskUseCase;
import com.example.tasks.domain.model.Task;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.example.tasks.adapters.inbound.http.TaskMappers.toResponse;

@Component
public class TaskFunctionHandlers {

    private final CreateTaskUseCase createTask;
    private final UpdateTaskUseCase updateTask;
    private final GetTaskByIdUseCase getTask;
    private final ListTasksByUserUseCase listTasks;
    private final DeleteTaskUseCase deleteTask;

    public TaskFunctionHandlers(CreateTaskUseCase createTask,
                                UpdateTaskUseCase updateTask,
                                GetTaskByIdUseCase getTask,
                                ListTasksByUserUseCase listTasks,
                                DeleteTaskUseCase deleteTask) {
        this.createTask = createTask;
        this.updateTask = updateTask;
        this.getTask = getTask;
        this.listTasks = listTasks;
        this.deleteTask = deleteTask;
    }

    @Bean
    public Function<AuthorizedRequest<CreateTaskRequest>, ResponseEntity<TaskResponse>> createTask() {
        return req -> {
            CreateTaskRequest body = req.body();
            Task draft = new Task(null, req.userId(), body.description(), body.priority(), null, req.now(), req.now());
            Task created = createTask.create(draft);
            return new ResponseEntity<>(toResponse(created), HttpStatus.CREATED);
        };
    }

    @Bean
    public Function<AuthorizedRequest<UpdateTaskRequest>, ResponseEntity<TaskResponse>> updateTask() {
        return req -> {
            UpdateTaskRequest body = req.body();
            Task upd = new Task(null, req.userId(), body.description(), body.priority(), body.status(), req.now(), req.now());
            Task updated = updateTask.update(req.pathParam("id"), req.userId(), upd);
            if (updated == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            return ResponseEntity.ok(toResponse(updated));
        };
    }

    @Bean
    public Function<AuthorizedRequest<Void>, ResponseEntity<TaskResponse>> getTaskById() {
        return req -> {
            Optional<Task> t = getTask.get(req.pathParam("id"), req.userId());
            return t.map(value -> ResponseEntity.ok(toResponse(value)))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        };
    }

    @Bean
    public Function<AuthorizedRequest<Void>, ResponseEntity<List<TaskResponse>>> listTasksByUser() {
        return req -> {
            int page = Integer.parseInt(req.queryParamOrDefault("page", "0"));
            int size = Integer.parseInt(req.queryParamOrDefault("size", "20"));
            List<TaskResponse> list = listTasks.list(req.userId(), page, size).stream().map(TaskMappers::toResponse).toList();
            return ResponseEntity.ok(list);
        };
    }

    @Bean
    public Function<AuthorizedRequest<Void>, ResponseEntity<Void>> deleteTask() {
        return req -> {
            boolean deleted = deleteTask.delete(req.pathParam("id"), req.userId());
            return deleted ? new ResponseEntity<>(HttpStatus.NO_CONTENT) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
        };
    }
}

