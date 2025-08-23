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
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.example.tasks.adapters.inbound.http.TaskMappers.toResponse;

@Component
public class TaskFunctionHandlers {

    private static final String USER_HEADER = "X-User-Id";

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
    public Function<Message<CreateTaskRequest>, Message<ResponseEntity<TaskResponse>>> createTask() {
        return msg -> {
            String userId = msg.getHeaders().get(USER_HEADER, String.class);
            CreateTaskRequest body = msg.getPayload();
            Task draft = new Task(null, userId, body.description(), body.priority(), null, Instant.now(), Instant.now());
            Task created = createTask.create(draft);
            ResponseEntity<TaskResponse> resp = new ResponseEntity<>(toResponse(created), HttpStatus.CREATED);
            return MessageBuilder.withPayload(resp).copyHeaders(msg.getHeaders()).build();
        };
    }

    @Bean
    public Function<Message<UpdateTaskRequest>, Message<ResponseEntity<TaskResponse>>> updateTask() {
        return msg -> {
            String userId = msg.getHeaders().get(USER_HEADER, String.class);
            String id = query(msg.getHeaders(), "id");
            UpdateTaskRequest body = msg.getPayload();
            Task upd = new Task(null, userId, body.description(), body.priority(), body.status(), Instant.now(), Instant.now());
            Task updated = updateTask.update(id, userId, upd);
            ResponseEntity<TaskResponse> resp = (updated == null)
                    ? new ResponseEntity<>(HttpStatus.NOT_FOUND)
                    : ResponseEntity.ok(toResponse(updated));
            return MessageBuilder.withPayload(resp).copyHeaders(msg.getHeaders()).build();
        };
    }

    @Bean
    public Function<Message<Void>, Message<ResponseEntity<TaskResponse>>> getTaskById() {
        return msg -> {
            String userId = msg.getHeaders().get(USER_HEADER, String.class);
            String id = query(msg.getHeaders(), "id");
            Optional<Task> t = getTask.get(id, userId);
            ResponseEntity<TaskResponse> resp = t.map(value -> ResponseEntity.ok(toResponse(value)))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
            return MessageBuilder.withPayload(resp).copyHeaders(msg.getHeaders()).build();
        };
    }

    @Bean
    public Function<Message<Void>, Message<ResponseEntity<List<TaskResponse>>>> listTasksByUser() {
        return msg -> {
            String userId = msg.getHeaders().get(USER_HEADER, String.class);
            int page = Integer.parseInt(queryOrDefault(msg.getHeaders(), "page", "0"));
            int size = Integer.parseInt(queryOrDefault(msg.getHeaders(), "size", "20"));
            List<TaskResponse> list = listTasks.list(userId, page, size).stream().map(TaskMappers::toResponse).toList();
            ResponseEntity<List<TaskResponse>> resp = ResponseEntity.ok(list);
            return MessageBuilder.withPayload(resp).copyHeaders(msg.getHeaders()).build();
        };
    }

    @Bean
    public Function<Message<Void>, Message<ResponseEntity<Void>>> deleteTask() {
        return msg -> {
            String userId = msg.getHeaders().get(USER_HEADER, String.class);
            String id = query(msg.getHeaders(), "id");
            boolean deleted = deleteTask.delete(id, userId);
            ResponseEntity<Void> resp = deleted ? new ResponseEntity<>(HttpStatus.NO_CONTENT) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
            return MessageBuilder.withPayload(resp).copyHeaders(msg.getHeaders()).build();
        };
    }

    private String queryOrDefault(Map<String, Object> headers, String key, String def) {
        String v = query(headers, key);
        return v != null ? v : def;
    }

    private String query(Map<String, Object> headers, String key) {
        Object v = headers.get(key);
        if (v == null) v = headers.get("http_query_" + key);
        if (v == null) v = headers.get("query_" + key);
        return v != null ? v.toString() : null;
    }
}