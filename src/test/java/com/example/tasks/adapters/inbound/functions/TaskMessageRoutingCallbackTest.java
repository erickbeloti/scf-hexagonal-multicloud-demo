package com.example.tasks.adapters.inbound.functions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.List;

class TaskMessageRoutingCallbackTest {

    private final TaskMessageRoutingCallback routingCallback = new TaskMessageRoutingCallback();

    @Nested
    @DisplayName("Function Name Header Routing Tests")
    class FunctionNameHeaderRoutingTests {

        @Test
        @DisplayName("Should route based on function.name header as String")
        void shouldRouteByFunctionNameHeaderAsString() {
            Message<String> message = MessageBuilder.withPayload("test")
                .setHeader("function.name", "updateTask")
                .build();

            String result = routingCallback.routingResult(message);

            assertThat(result).isEqualTo("updateTask");
        }

        @Test
        @DisplayName("Should route based on function.name header as List")
        void shouldRouteByFunctionNameHeaderAsList() {
            Message<String> message = MessageBuilder.withPayload("test")
                .setHeader("function.name", List.of("getTaskById"))
                .build();

            String result = routingCallback.routingResult(message);

            assertThat(result).isEqualTo("getTaskById");
        }

        @Test
        @DisplayName("Should route to first element when function.name header has multiple values")
        void shouldRouteToFirstElementWhenMultipleValues() {
            Message<String> message = MessageBuilder.withPayload("test")
                .setHeader("function.name", List.of("deleteTask", "updateTask"))
                .build();

            String result = routingCallback.routingResult(message);

            assertThat(result).isEqualTo("deleteTask");
        }

        @Test
        @DisplayName("Should handle empty function.name list")
        void shouldHandleEmptyFunctionNameList() {
            Message<String> message = MessageBuilder.withPayload("test")
                .setHeader("function.name", List.of())
                .build();

            String result = routingCallback.routingResult(message);

            assertThat(result).isEqualTo("defaultFunction");
        }

        @Test
        @DisplayName("Should handle empty function.name string")
        void shouldHandleEmptyFunctionNameString() {
            Message<String> message = MessageBuilder.withPayload("test")
                .setHeader("function.name", "")
                .build();

            String result = routingCallback.routingResult(message);

            assertThat(result).isEqualTo("defaultFunction");
        }

        @Test
        @DisplayName("Should handle whitespace-only function.name string")
        void shouldHandleWhitespaceOnlyFunctionNameString() {
            Message<String> message = MessageBuilder.withPayload("test")
                .setHeader("function.name", "   ")
                .build();

            String result = routingCallback.routingResult(message);

            assertThat(result).isEqualTo("defaultFunction");
        }
    }

    @Nested
    @DisplayName("Fallback Routing Tests")
    class FallbackRoutingTests {

        @Test
        @DisplayName("Should fallback to defaultFunction when no function.name header")
        void shouldFallbackToCreateTaskWhenNoHeader() {
            Message<String> message = MessageBuilder.withPayload("test").build();

            String result = routingCallback.routingResult(message);

            assertThat(result).isEqualTo("defaultFunction");
        }

        @Test
        @DisplayName("Should fallback to defaultFunction for null function.name header")
        void shouldFallbackToCreateTaskForNullHeader() {
            Message<String> message = MessageBuilder.withPayload("test")
                .setHeader("function.name", null)
                .build();

            String result = routingCallback.routingResult(message);

            assertThat(result).isEqualTo("defaultFunction");
        }

        @Test
        @DisplayName("Should fallback to defaultFunction for unknown header type")
        void shouldFallbackToCreateTaskForUnknownHeaderType() {
            Message<String> message = MessageBuilder.withPayload("test")
                .setHeader("function.name", 123)  // Integer instead of String or List
                .build();

            String result = routingCallback.routingResult(message);

            assertThat(result).isEqualTo("defaultFunction");
        }
    }

    @Nested
    @DisplayName("Valid Function Names")
    class ValidFunctionNamesTests {

        @Test
        @DisplayName("Should route to createTask")
        void shouldRouteToCreateTask() {
            Message<String> message = MessageBuilder.withPayload("test")
                .setHeader("function.name", "createTask")
                .build();

            String result = routingCallback.routingResult(message);

            assertThat(result).isEqualTo("createTask");
        }

        @Test
        @DisplayName("Should route to updateTask")
        void shouldRouteToUpdateTask() {
            Message<String> message = MessageBuilder.withPayload("test")
                .setHeader("function.name", "updateTask")
                .build();

            String result = routingCallback.routingResult(message);

            assertThat(result).isEqualTo("updateTask");
        }

        @Test
        @DisplayName("Should route to getTaskById")
        void shouldRouteToGetTaskById() {
            Message<String> message = MessageBuilder.withPayload("test")
                .setHeader("function.name", "getTaskById")
                .build();

            String result = routingCallback.routingResult(message);

            assertThat(result).isEqualTo("getTaskById");
        }

        @Test
        @DisplayName("Should route to listTasksByUser")
        void shouldRouteToListTasksByUser() {
            Message<String> message = MessageBuilder.withPayload("test")
                .setHeader("function.name", "listTasksByUser")
                .build();

            String result = routingCallback.routingResult(message);

            assertThat(result).isEqualTo("listTasksByUser");
        }

        @Test
        @DisplayName("Should route to deleteTask")
        void shouldRouteToDeleteTask() {
            Message<String> message = MessageBuilder.withPayload("test")
                .setHeader("function.name", "deleteTask")
                .build();

            String result = routingCallback.routingResult(message);

            assertThat(result).isEqualTo("deleteTask");
        }
    }
}
