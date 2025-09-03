package com.example.tasks.adapters.inbound.functions.infrastructure;

import com.example.tasks.adapters.inbound.functions.dto.ResponseWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CloudResponseHelper to verify status code header is set correctly
 */
@ExtendWith(MockitoExtension.class)
class CloudResponseHelperTest {

    private CloudResponseHelper cloudResponseHelper;

    @BeforeEach
    void setUp() {
        cloudResponseHelper = new CloudResponseHelper();
        // Initialize the statusCodeHeader field since @Value annotation doesn't work in unit tests
        ReflectionTestUtils.setField(cloudResponseHelper, "statusCodeHeader", "statusCode");
    }

    @Test
    void shouldSetDefaultStatusCodeHeader() {
        // Given - default header name (statusCode)
        ResponseWrapper<String> payload = ResponseWrapper.<String>builder()
                .status(422)
                .data("test data")
                .build();

        // When
        Message<ResponseWrapper<String>> result = cloudResponseHelper.createResponse(payload, 422);

        // Then
        assertThat(result.getPayload()).isEqualTo(payload);
        assertThat(result.getHeaders()).containsEntry("statusCode", 422);
    }

    @Test
    void shouldSetCustomStatusCodeHeader() {
        // Given - custom header name
        ReflectionTestUtils.setField(cloudResponseHelper, "statusCodeHeader", "customStatusHeader");

        ResponseWrapper<String> payload = ResponseWrapper.<String>builder()
                .status(404)
                .data("not found")
                .build();

        // When
        Message<ResponseWrapper<String>> result = cloudResponseHelper.createResponse(payload, 404);

        // Then
        assertThat(result.getPayload()).isEqualTo(payload);
        assertThat(result.getHeaders()).containsEntry("customStatusHeader", 404);
        assertThat(result.getHeaders()).doesNotContainKey("statusCode");
    }

    @Test
    void shouldHandleDifferentStatusCodes() {
        // Given
        ResponseWrapper<Object> payload = ResponseWrapper.builder()
                .status(201)
                .build();

        // When
        Message<ResponseWrapper<Object>> result = cloudResponseHelper.createResponse(payload, 201);

        // Then
        assertThat(result.getHeaders()).containsEntry("statusCode", 201);
    }

    @Test
    void shouldSetHeaderForSuccessfulResponse() {
        // Given
        ResponseWrapper<String> payload = ResponseWrapper.<String>builder()
                .status(200)
                .data("success")
                .build();

        // When
        Message<ResponseWrapper<String>> result = cloudResponseHelper.createResponse(payload, 200);

        // Then
        assertThat(result.getHeaders()).containsEntry("statusCode", 200);
    }
}
