package com.example.tasks.adapters.inbound.functions;

import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskMessageRoutingCallback implements MessageRoutingCallback {
    @Override
    public String routingResult(Message<?> message) {
        Object functionName = message.getHeaders().get("function.name");

        String result = extractFunctionName(functionName);
        return result != null && !result.trim().isEmpty() ? result.trim() : "defaultFunction";
    }

    private String extractFunctionName(Object functionName) {
        if (functionName instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            return first instanceof String ? (String) first : null;
        } else if (functionName instanceof String) {
            return (String) functionName;
        }
        return null;
    }
}
