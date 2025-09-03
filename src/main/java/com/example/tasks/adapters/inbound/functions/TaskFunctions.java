package com.example.tasks.adapters.inbound.functions;

import com.example.tasks.adapters.inbound.functions.dto.CreateTaskRequest;
import com.example.tasks.adapters.inbound.functions.dto.DeleteTaskRequest;
import com.example.tasks.adapters.inbound.functions.dto.GetTaskRequest;
import com.example.tasks.adapters.inbound.functions.dto.ListTasksRequest;
import com.example.tasks.adapters.inbound.functions.dto.ResponseWrapper;
import com.example.tasks.adapters.inbound.functions.dto.TaskDtoMapper;
import com.example.tasks.adapters.inbound.functions.dto.UpdateTaskRequest;
import com.example.tasks.adapters.inbound.functions.infrastructure.CloudResponseHelper;
import com.example.tasks.application.service.TaskService;
import com.example.tasks.domain.Task;
import com.example.tasks.domain.TaskId;
import com.example.tasks.domain.UserId;
import com.example.tasks.domain.exception.TaskBusinessRuleException;
import com.example.tasks.domain.exception.TaskDomainException;
import com.example.tasks.infrastructure.logging.ApplicationLogger;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class TaskFunctions {

    private final TaskService service;
    private final Validator validator;
    private final CloudResponseHelper responseHelper;
    private final ApplicationLogger logger;

    public TaskFunctions(TaskService service, Validator validator,
                        CloudResponseHelper responseHelper, ApplicationLogger logger) {
        this.service = service;
        this.validator = validator;
        this.responseHelper = responseHelper;
        this.logger = logger;
    }

    @Bean
    public Function<Object, Message<ResponseWrapper<Object>>> defaultFunction() {
        return x -> createErrorResponse(404, "FUNCTION_NOT_FOUND", "Function not found");
    }

    @Bean
    public Function<CreateTaskRequest, Message<ResponseWrapper<Object>>> createTask() {
        return request -> {
            try {
                validateRequest(request);

                Task task = service.createTask(
                    UserId.of(request.userId()),
                    request.description(),
                    request.priority()
                );

                return createSuccessResponse(TaskDtoMapper.toResponse(task));
            } catch (Exception e) {
                return handleException(e);
            }
        };
    }

    @Bean
    public Function<UpdateTaskRequest, Message<ResponseWrapper<Object>>> updateTask() {
        return request -> {
            try {
                validateRequest(request);

                Task task = service.updateTask(
                    TaskId.of(request.id()),
                    UserId.of(request.userId()),
                    request.description(),
                    request.priority(),
                    request.status()
                );

                return createSuccessResponse(TaskDtoMapper.toResponse(task));

            } catch (Exception e) {
                return handleException(e);
            }
        };
    }

    @Bean
    public Function<GetTaskRequest, Message<ResponseWrapper<Object>>> getTaskById() {
        return request -> {
            try {
                validateRequest(request);

                Task task = service.getTask(
                    TaskId.of(request.id()),
                    UserId.of(request.userId())
                );

                return createSuccessResponse(TaskDtoMapper.toResponse(task));

            } catch (Exception e) {
                return handleException(e);
            }
        };
    }

    @Bean
    public Function<ListTasksRequest, Message<ResponseWrapper<Object>>> listTasksByUser() {
        return request -> {
            try {
                validateRequest(request);

                var pagedTasks = service.listTasks(
                    UserId.of(request.userId()),
                    request.page(),
                    request.size()
                );

                return createSuccessResponse(TaskDtoMapper.toPagedResponse(pagedTasks));

            } catch (Exception e) {
                return handleException(e);
            }
        };
    }

    @Bean
    public Function<DeleteTaskRequest, Message<ResponseWrapper<Object>>> deleteTask() {
        return request -> {
            try {
                validateRequest(request);

                service.deleteTask(
                    TaskId.of(request.id()),
                    UserId.of(request.userId())
                );

                var response = ResponseWrapper.<Object>builder()
                        .status(204)
                        .build();
                return responseHelper.createResponse(response, 204);

            } catch (Exception e) {
                return handleException(e);
            }
        };
    }

    private <T> void validateRequest(T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Validation failed: " +
                violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", ")));
        }
    }

    private Message<ResponseWrapper<Object>> handleException(Exception e) {
        logger.error("Exception occurred in function execution: {}", e.getMessage(), e);

        if (e instanceof TaskBusinessRuleException) {
            logger.warn("Business rule violation: {}", e.getMessage());
            return createErrorResponse(422, "BUSINESS_RULE_VIOLATION", e.getMessage());
        }

        if (e instanceof TaskDomainException domainEx) {
            logger.warn("Domain exception: {} - {}", domainEx.getErrorCode(), domainEx.getMessage());
            int status = mapDomainExceptionToStatus(domainEx);
            return createErrorResponse(status, domainEx.getErrorCode(), domainEx.getMessage());
        }

        if (e instanceof IllegalArgumentException && e.getMessage().startsWith("Validation failed")) {
            logger.warn("Validation error: {}", e.getMessage());
            return createValidationErrorResponse(e.getMessage());
        }

        logger.error("Unexpected error occurred", e);
        return createErrorResponse(500, "INTERNAL_ERROR", "An unexpected error occurred: " + e.getMessage());
    }

    private Message<ResponseWrapper<Object>> createSuccessResponse(Object data) {
        var response = ResponseWrapper.<Object>builder()
                .status(200)
                .data(data)
                .build();
        return responseHelper.createResponse(response, 200);
    }

    private Message<ResponseWrapper<Object>> createErrorResponse(int status, String code, String message) {
        var response = ResponseWrapper.<Object>builder()
                .status(status)
                .errors(List.of(com.example.tasks.adapters.inbound.functions.dto.Error.builder()
                    .code(code)
                    .message(message)
                    .build()))
                .build();
        return responseHelper.createResponse(response, status);
    }

    private Message<ResponseWrapper<Object>> createValidationErrorResponse(String validationMessage) {
        var response = ResponseWrapper.<Object>builder()
                .status(400)
                .errors(List.of(com.example.tasks.adapters.inbound.functions.dto.Error.builder()
                    .code("VALIDATION_ERROR")
                    .message(validationMessage)
                    .build()))
                .message("Validation failed")
                .build();
        return responseHelper.createResponse(response, 400);
    }

    private int mapDomainExceptionToStatus(TaskDomainException e) {
        return switch (e.getErrorCode()) {
            case "TASK_NOT_FOUND" -> 404;
            case "TASK_ACCESS_DENIED" -> 403;
            default -> 422;
        };
    }
}
