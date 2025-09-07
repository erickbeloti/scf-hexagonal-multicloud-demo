package com.example.tasks.adapters.inbound.functions;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.tasks.adapters.inbound.functions.dto.*;
import com.example.tasks.adapters.inbound.functions.infrastructure.CloudResponseHelper;
import com.example.tasks.application.service.TaskService;
import com.example.tasks.domain.*;
import com.example.tasks.domain.exception.*;
import com.example.tasks.infrastructure.logging.ApplicationLogger;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.messaging.Message;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

class FunctionInvocationTest {

    TaskService taskService;
    Validator validator;
    CloudResponseHelper responseHelper;
    ApplicationLogger functionLogger;
    TaskFunctions taskFunctions;

    UserId userId;
    UserId otherUserId;
    TaskId taskId;
    LocalDateTime now;

    @BeforeEach
    void setup() {
        taskService = mock(TaskService.class);
        validator = mock(Validator.class);
        responseHelper = mock(CloudResponseHelper.class);
        functionLogger = mock(ApplicationLogger.class);
        taskFunctions = new TaskFunctions(taskService, validator, responseHelper, functionLogger);

        userId = UserId.of("user123");
        otherUserId = UserId.of("otherUser");
        taskId = TaskId.of("123e4567-e89b-12d3-a456-426614174000");
        now = LocalDateTime.now();
    }

    @Nested
    @DisplayName("Create Task Function")
    class CreateTaskFunctionTests {

        @Test
        @DisplayName("Should create task successfully and return proper response")
        void shouldCreateTaskSuccessfullyAndReturnProperResponse() {
            // Given
            CreateTaskRequest request = new CreateTaskRequest(
                userId.value(),
                "Test task description",
                Priority.HIGH
            );

            Task expectedTask = Task.reconstitute(
                taskId,
                userId,
                "Test task description",
                Priority.HIGH,
                Status.OPEN,
                now,
                now
            );

            when(validator.validate(any())).thenReturn(Collections.emptySet());
            when(taskService.createTask(any(UserId.class), anyString(), any(Priority.class)))
                .thenReturn(expectedTask);

            ResponseWrapper<Object> expectedResponse = ResponseWrapper.<Object>builder()
                .status(200)
                .data(TaskDtoMapper.toResponse(expectedTask))
                .build();

            Message<ResponseWrapper<Object>> expectedMessage = mock(Message.class);
            when(responseHelper.createResponse(any(), eq(200))).thenReturn(expectedMessage);

            // When
            var result = taskFunctions.createTask().apply(request);

            // Then
            assertThat(result).isEqualTo(expectedMessage);
            verify(taskService).createTask(userId, "Test task description", Priority.HIGH);
            verify(responseHelper).createResponse(any(ResponseWrapper.class), eq(200));
        }

        @Test
        @DisplayName("Should handle validation errors")
        void shouldHandleValidationErrors() {
            // Given
            CreateTaskRequest request = new CreateTaskRequest(
                "",
                "",
                null
            );

            ConstraintViolation<CreateTaskRequest> violation = mock(ConstraintViolation.class);
            when(violation.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
            when(violation.getPropertyPath().toString()).thenReturn("description");
            when(violation.getMessage()).thenReturn("Description cannot be empty");

            when(validator.validate(request)).thenReturn(Set.of(violation));

            Message<ResponseWrapper<Object>> expectedMessage = mock(Message.class);
            when(responseHelper.createResponse(any(), eq(400))).thenReturn(expectedMessage);

            // When
            var result = taskFunctions.createTask().apply(request);

            // Then
            assertThat(result).isEqualTo(expectedMessage);
            verify(responseHelper).createResponse(any(ResponseWrapper.class), eq(400));
        }

        @Test
        @DisplayName("Should handle business rule violations")
        void shouldHandleBusinessRuleViolations() {
            // Given
            CreateTaskRequest request = new CreateTaskRequest(
                userId.value(),
                "Test task description",
                Priority.HIGH
            );

            when(validator.validate(any())).thenReturn(Collections.emptySet());
            when(taskService.createTask(any(UserId.class), anyString(), any(Priority.class)))
                .thenThrow(new TaskBusinessRuleException("Too many high priority tasks"));

            Message<ResponseWrapper<Object>> expectedMessage = mock(Message.class);
            when(responseHelper.createResponse(any(), eq(422))).thenReturn(expectedMessage);

            // When
            var result = taskFunctions.createTask().apply(request);

            // Then
            assertThat(result).isEqualTo(expectedMessage);
            verify(responseHelper).createResponse(any(ResponseWrapper.class), eq(422));
        }
    }

    @Nested
    @DisplayName("Get Task Function")
    class GetTaskFunctionTests {

        @Test
        @DisplayName("Should get task successfully")
        void shouldGetTaskSuccessfully() {
            // Given
            GetTaskRequest request = new GetTaskRequest(taskId.value(), userId.value());

            Task expectedTask = Task.reconstitute(
                taskId,
                userId,
                "Test task description",
                Priority.HIGH,
                Status.OPEN,
                now,
                now
            );

            when(validator.validate(any())).thenReturn(Collections.emptySet());
            when(taskService.getTask(taskId, userId)).thenReturn(expectedTask);

            Message<ResponseWrapper<Object>> expectedMessage = mock(Message.class);
            when(responseHelper.createResponse(any(), eq(200))).thenReturn(expectedMessage);

            // When
            var result = taskFunctions.getTaskById().apply(request);

            // Then
            assertThat(result).isEqualTo(expectedMessage);
            verify(taskService).getTask(taskId, userId);
            verify(responseHelper).createResponse(any(ResponseWrapper.class), eq(200));
        }

        @Test
        @DisplayName("Should handle task not found")
        void shouldHandleTaskNotFound() {
            // Given
            GetTaskRequest request = new GetTaskRequest(taskId.value(), userId.value());

            when(validator.validate(any())).thenReturn(Collections.emptySet());
            when(taskService.getTask(taskId, userId))
                .thenThrow(new TaskNotFoundException("Task not found"));

            Message<ResponseWrapper<Object>> expectedMessage = mock(Message.class);
            when(responseHelper.createResponse(any(), eq(404))).thenReturn(expectedMessage);

            // When
            var result = taskFunctions.getTaskById().apply(request);

            // Then
            assertThat(result).isEqualTo(expectedMessage);
            verify(responseHelper).createResponse(any(ResponseWrapper.class), eq(404));
        }
    }
}
