package com.aqa.support;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Отправляет запросы тестируемому приложению и прикладывает запрос и ответ к отчету. */
public final class ApiClient {

    private static final URI ENDPOINT = URI.create("http://localhost:" + TestEnvironment.APP_PORT + "/endpoint");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private ApiClient() {
    }

    /** Обычный запрос с правильным ключом. */
    public static ApiResponse send(String token, String action) {
        return send(token, action, TestEnvironment.API_KEY);
    }

    /** Запрос с заданным ключом; apiKey = null означает «без заголовка». */
    @Step("Отправить запрос: токен={token}, действие={action}")
    public static ApiResponse send(String token, String action, String apiKey) {
        Map<String, String> form = new LinkedHashMap<>();
        if (token != null) {
            form.put("token", token);
        }
        if (action != null) {
            form.put("action", action);
        }
        return post(formBody(form), "application/x-www-form-urlencoded", apiKey);
    }

    @Step("Отправить запрос с телом «{body}» и типом {contentType}")
    public static ApiResponse post(String body, String contentType, String apiKey) {
        HttpRequest.Builder request = HttpRequest.newBuilder(ENDPOINT)
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", contentType)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (apiKey != null) {
            request.header("X-Api-Key", apiKey);
        }
        return execute(request.build(), body);
    }

    @Step("Отправить GET-запрос вместо POST")
    public static ApiResponse get() {
        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .header("Accept", "application/json")
                .header("X-Api-Key", TestEnvironment.API_KEY)
                .GET().build();
        return execute(request, "");
    }

    @Step("Отправить запрос на другой адрес {path}")
    public static ApiResponse postTo(String path, String body) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + TestEnvironment.APP_PORT + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .header("X-Api-Key", TestEnvironment.API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return execute(request, body);
    }

    private static ApiResponse execute(HttpRequest request, String body) {
        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            ApiResponse result = new ApiResponse(response.statusCode(), response.body(),
                    response.headers().firstValue("Content-Type").orElse(""));
            Allure.addAttachment("Запрос", "text/plain",
                    request.method() + " " + request.uri() + "\n" + request.headers().map() + "\n\n" + body);
            Allure.addAttachment("Ответ", "text/plain", "HTTP " + result.status() + "\n\n" + result.body());
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось отправить запрос приложению", e);
        }
    }

    private static String formBody(Map<String, String> form) {
        StringBuilder sb = new StringBuilder();
        form.forEach((k, v) -> {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8)).append('=')
                    .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }
}
