package com.example.tasks.adapters.inbound.http;

import java.time.Instant;
import java.util.Map;

public record AuthorizedRequest<T>(
        String userId,
        T body,
        Map<String, String> pathParams,
        Map<String, String> queryParams,
        Instant now
) {
    public String pathParam(String key) { return pathParams.get(key); }
    public String queryParamOrDefault(String key, String defaultValue) { return queryParams.getOrDefault(key, defaultValue); }
}

