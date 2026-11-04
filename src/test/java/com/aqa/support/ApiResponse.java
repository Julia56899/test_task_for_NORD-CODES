package com.aqa.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Ответ приложения: HTTP-код и разобранное тело. */
public record ApiResponse(int status, String body, String contentType) {

    private static final ObjectMapper JSON = new ObjectMapper();

    public String result() {
        return field("result");
    }

    public String message() {
        return field("message");
    }

    private String field(String name) {
        try {
            JsonNode node = JSON.readTree(body);
            return node != null && node.hasNonNull(name) ? node.get(name).asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
