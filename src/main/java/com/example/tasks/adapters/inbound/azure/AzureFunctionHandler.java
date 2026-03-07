package com.example.tasks.adapters.inbound.azure;

import com.example.tasks.adapters.inbound.functions.dto.CreateTaskRequest;
import com.example.tasks.adapters.inbound.functions.dto.DeleteTaskRequest;
import com.example.tasks.adapters.inbound.functions.dto.GetTaskRequest;
import com.example.tasks.adapters.inbound.functions.dto.ListTasksRequest;
import com.example.tasks.adapters.inbound.functions.dto.ResponseWrapper;
import com.example.tasks.adapters.inbound.functions.dto.UpdateTaskRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Azure Functions HTTP handler that delegates to Spring Cloud Functions.
 *
 * This handler uses constructor injection to receive Spring Cloud Function beans,
 * reusing the existing function beans defined in TaskFunctions.java.
 *
 * The routing follows the same pattern as AWS/GCP via the 'function.name' header.
 */
@Component
public class AzureFunctionHandler {

    private final Function<CreateTaskRequest, Message<ResponseWrapper<Object>>> createTask;
    private final Function<UpdateTaskRequest, Message<ResponseWrapper<Object>>> updateTask;
    private final Function<GetTaskRequest, Message<ResponseWrapper<Object>>> getTaskById;
    private final Function<ListTasksRequest, Message<ResponseWrapper<Object>>> listTasksByUser;
    private final Function<DeleteTaskRequest, Message<ResponseWrapper<Object>>> deleteTask;
    private final Function<Object, Message<ResponseWrapper<Object>>> defaultFunction;
    private final ObjectMapper objectMapper;

    /**
     * Map of function names to their request types for deserialization
     */
    private static final Map<String, Class<?>> FUNCTION_REQUEST_TYPES = Map.of(
        "createTask", CreateTaskRequest.class,
        "updateTask", UpdateTaskRequest.class,
        "getTaskById", GetTaskRequest.class,
        "listTasksByUser", ListTasksRequest.class,
        "deleteTask", DeleteTaskRequest.class
    );

    public AzureFunctionHandler(
            Function<CreateTaskRequest, Message<ResponseWrapper<Object>>> createTask,
            Function<UpdateTaskRequest, Message<ResponseWrapper<Object>>> updateTask,
            Function<GetTaskRequest, Message<ResponseWrapper<Object>>> getTaskById,
            Function<ListTasksRequest, Message<ResponseWrapper<Object>>> listTasksByUser,
            Function<DeleteTaskRequest, Message<ResponseWrapper<Object>>> deleteTask,
            Function<Object, Message<ResponseWrapper<Object>>> defaultFunction,
            ObjectMapper objectMapper) {
        this.createTask = createTask;
        this.updateTask = updateTask;
        this.getTaskById = getTaskById;
        this.listTasksByUser = listTasksByUser;
        this.deleteTask = deleteTask;
        this.defaultFunction = defaultFunction;
        this.objectMapper = objectMapper;
    }

    /**
     * HTTP Trigger that routes requests to Spring Cloud Functions based on the function.name header.
     *
     * Supported operations (via function.name header):
     * - createTask: Create a new task
     * - updateTask: Update an existing task
     * - getTaskById: Get a task by ID
     * - listTasksByUser: List tasks for a user
     * - deleteTask: Delete a task
     *
     * @param request The HTTP request
     * @param context The Azure execution context
     * @return HTTP response with the function result
     */
    @FunctionName("taskRouter")
    public HttpResponseMessage route(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE},
                authLevel = AuthorizationLevel.ANONYMOUS,
                route = "tasks/{*path}"
            ) HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {

        context.getLogger().info("Azure Function - Processing request: " + request.getHttpMethod());

        // Get function name from header (same pattern as AWS/GCP)
        String functionName = request.getHeaders().getOrDefault("function.name", "defaultFunction");
        context.getLogger().info("Azure Function - Routing to function: " + functionName);

        try {
            // Get the request body
            String body = request.getBody().orElse("{}");
            context.getLogger().info("Azure Function - Request body: " + body);

            // Invoke the appropriate function based on the function name
            Message<ResponseWrapper<Object>> result = invokeFunction(functionName, body, context);
            ResponseWrapper<Object> responseWrapper = result.getPayload();

            // Serialize response to JSON
            String responseBody = objectMapper.writeValueAsString(responseWrapper);

            context.getLogger().info("Azure Function - Response: " + responseBody);

            return request.createResponseBuilder(HttpStatus.valueOf(responseWrapper.getStatus()))
                .header("Content-Type", "application/json")
                .body(responseBody)
                .build();

        } catch (Exception e) {
            context.getLogger().severe("Azure Function - Error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type", "application/json")
                .body("{\"status\":500,\"errors\":[{\"code\":\"INTERNAL_ERROR\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}]}")
                .build();
        }
    }

    /**
     * Invokes the appropriate function based on the function name.
     * Deserializes the JSON body to the correct request type.
     */
    @SuppressWarnings("unchecked")
    private Message<ResponseWrapper<Object>> invokeFunction(String functionName, String body, ExecutionContext context) throws Exception {
        Class<?> requestType = FUNCTION_REQUEST_TYPES.get(functionName);

        if (requestType == null) {
            context.getLogger().warning("Azure Function - Unknown function: " + functionName + ", using defaultFunction");
            return defaultFunction.apply(body);
        }

        // Deserialize JSON to the appropriate request type
        Object requestObject = objectMapper.readValue(body, requestType);
        context.getLogger().info("Azure Function - Deserialized request to: " + requestType.getSimpleName());

        // Route to the appropriate function
        return switch (functionName) {
            case "createTask" -> createTask.apply((CreateTaskRequest) requestObject);
            case "updateTask" -> updateTask.apply((UpdateTaskRequest) requestObject);
            case "getTaskById" -> getTaskById.apply((GetTaskRequest) requestObject);
            case "listTasksByUser" -> listTasksByUser.apply((ListTasksRequest) requestObject);
            case "deleteTask" -> deleteTask.apply((DeleteTaskRequest) requestObject);
            default -> defaultFunction.apply(body);
        };
    }

    /**
     * Escapes special characters for JSON string.
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                   .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}

