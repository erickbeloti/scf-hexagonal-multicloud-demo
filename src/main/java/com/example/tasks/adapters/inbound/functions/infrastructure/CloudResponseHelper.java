package com.example.tasks.adapters.inbound.functions.infrastructure;

import com.example.tasks.adapters.inbound.functions.dto.ResponseWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Cloud response helper that uses a single configurable status code header
 */
@Component
public class CloudResponseHelper {

    @Value("${functions.status-header:statusCode}")
    private String statusCodeHeader;

    /**
     * Creates a response with the configured status code header
     */
    public <T> Message<ResponseWrapper<T>> createResponse(ResponseWrapper<T> payload, int statusCode) {
        return MessageBuilder.withPayload(payload)
                .setHeader(statusCodeHeader, statusCode)
                .build();
    }
}
