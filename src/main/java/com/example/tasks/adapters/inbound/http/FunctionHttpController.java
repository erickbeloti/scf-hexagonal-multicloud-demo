package com.example.tasks.adapters.inbound.http;

import com.example.tasks.adapters.inbound.http.dto.TaskDtos.CreateTaskRequest;
import com.example.tasks.adapters.inbound.http.dto.TaskDtos.TaskResponse;
import com.example.tasks.adapters.inbound.http.dto.TaskDtos.UpdateTaskRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@RestController
@RequestMapping("/functions")
public class FunctionHttpController {

    private final FunctionCatalog catalog;
    private final ObjectMapper objectMapper;

    public FunctionHttpController(FunctionCatalog catalog, ObjectMapper objectMapper) {
        this.catalog = catalog;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/createTask")
    public ResponseEntity<TaskResponse> create(@RequestHeader("X-User-Id") String userId,
                                               @RequestBody @Valid CreateTaskRequest body) {
        var function = catalog.lookup(Function.class, "createTask");
        var req = authorized(userId, body, Map.of(), Map.of());
        return (ResponseEntity<TaskResponse>) function.apply(req);
    }

    @PutMapping("/updateTask/{id}")
    public ResponseEntity<TaskResponse> update(@RequestHeader("X-User-Id") String userId,
                                               @PathVariable String id,
                                               @RequestBody @Valid UpdateTaskRequest body) {
        var function = catalog.lookup(Function.class, "updateTask");
        var req = authorized(userId, body, Map.of("id", id), Map.of());
        return (ResponseEntity<TaskResponse>) function.apply(req);
    }

    @GetMapping("/getTaskById/{id}")
    public ResponseEntity<TaskResponse> get(@RequestHeader("X-User-Id") String userId,
                                            @PathVariable String id) {
        var function = catalog.lookup(Function.class, "getTaskById");
        var req = authorized(userId, null, Map.of("id", id), Map.of());
        return (ResponseEntity<TaskResponse>) function.apply(req);
    }

    @GetMapping("/listTasksByUser")
    public ResponseEntity<?> list(@RequestHeader("X-User-Id") String userId,
                                  @RequestParam(defaultValue = "0") String page,
                                  @RequestParam(defaultValue = "20") String size) {
        var function = catalog.lookup(Function.class, "listTasksByUser");
        var req = authorized(userId, null, Map.of(), Map.of("page", page, "size", size));
        return (ResponseEntity<?>) function.apply(req);
    }

    @DeleteMapping("/deleteTask/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("X-User-Id") String userId,
                                       @PathVariable String id) {
        var function = catalog.lookup(Function.class, "deleteTask");
        var req = authorized(userId, null, Map.of("id", id), Map.of());
        return (ResponseEntity<Void>) function.apply(req);
    }

    private <T> AuthorizedRequest<T> authorized(String userId, T body, Map<String, String> path, Map<String, String> query) {
        Objects.requireNonNull(userId, "X-User-Id header is required");
        return new AuthorizedRequest<>(userId, body, path, query, Instant.now());
    }
}

