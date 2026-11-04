package com.aqa.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import io.qameta.allure.Step;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Действия и проверки, названные так, чтобы отчет читался без знания кода. */
public final class Steps {

    private Steps() {
    }

    @Step("Пользователь входит в систему (LOGIN)")
    public static ApiResponse login(String token) {
        return ApiClient.send(token, "LOGIN");
    }

    @Step("Пользователь выполняет действие (ACTION)")
    public static ApiResponse action(String token) {
        return ApiClient.send(token, "ACTION");
    }

    @Step("Пользователь выходит из системы (LOGOUT)")
    public static ApiResponse logout(String token) {
        return ApiClient.send(token, "LOGOUT");
    }

    @Step("Проверить: запрос выполнен успешно (HTTP 200, result = OK)")
    public static void assertSuccess(ApiResponse response) {
        assertEquals(200, response.status(), "HTTP-код ответа. Тело ответа: " + response.body());
        assertEquals("OK", response.result(), "Поле result. Тело ответа: " + response.body());
    }

    @Step("Проверить: запрос отклонен с ошибкой (result = ERROR) и есть пояснение")
    public static void assertError(ApiResponse response) {
        assertEquals("ERROR", response.result(), "Поле result. Тело ответа: " + response.body());
        assertNotNull(response.message(), "Поле message с причиной ошибки. Тело ответа: " + response.body());
        assertTrue(!response.message().isBlank(), "Причина ошибки не должна быть пустой");
    }

    @Step("Проверить: HTTP-код ответа равен {expected}")
    public static void assertStatus(ApiResponse response, int expected) {
        assertEquals(expected, response.status(), "HTTP-код ответа. Тело ответа: " + response.body());
    }

    @Step("Проверить: приложение обратилось к внешнему сервису {path} ровно {times} раз(а)")
    public static void assertExternalCalls(WireMockServer service, String path, int times) {
        service.verify(times, postRequestedFor(urlEqualTo(path)));
    }

    public static RequestPatternBuilder externalRequest(String path) {
        return WireMock.postRequestedFor(urlEqualTo(path));
    }
}
