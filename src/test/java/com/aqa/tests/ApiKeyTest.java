package com.aqa.tests;

import com.aqa.support.ApiClient;
import com.aqa.support.ApiResponse;
import com.aqa.support.BaseTest;
import com.aqa.support.Steps;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Epic("Сервис авторизации")
@Feature("Защита доступа по API-ключу")
class ApiKeyTest extends BaseTest {

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Запрос без API-ключа отклоняется")
    @Description("Без заголовка X-Api-Key сервис не должен выполнять никаких действий и должен ответить «не авторизован» (401).")
    void requestWithoutKeyIsRejected() {
        // шлем запрос совсем без заголовка с ключом, ждем 401 и что наружу ничего не ушло
        ApiResponse response = ApiClient.send(newToken(), "LOGIN", null);

        Steps.assertStatus(response, 401);
        Steps.assertError(response);
        Steps.assertExternalCalls(externalService, "/auth", 0);
    }

    @ParameterizedTest(name = "Неверный ключ «{0}» отклоняется")
    @ValueSource(strings = {"wrong", "qazwsxedc", "QAZWSXEDC", "qazWSXedc1", "qazWSXed"})
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Запрос с неверным API-ключом отклоняется")
    @Description("Ключ сравнивается строго: другой регистр, недостающий или добавленный символ - это уже неверный ключ.")
    void requestWithWrongKeyIsRejected(String wrongKey) {
        // ключ сравнивается строго, поэтому любое отличие должно давать 401
        ApiResponse response = ApiClient.send(newToken(), "LOGIN", wrongKey);

        Steps.assertStatus(response, 401);
        Steps.assertError(response);
        Steps.assertExternalCalls(externalService, "/auth", 0);
    }

    @ParameterizedTest(name = "Действие {0} без ключа отклоняется")
    @ValueSource(strings = {"LOGIN", "ACTION", "LOGOUT"})
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Ключ требуется для каждого из трех действий")
    void everyActionRequiresKey(String action) {
        // проверяем все три действия, чтобы не было лазейки у какого-то одного
        ApiResponse response = ApiClient.send(newToken(), action, null);

        Steps.assertStatus(response, 401);
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Запрос с правильным ключом доходит до обработки")
    @Description("С верным ключом сервис не отвечает «не авторизован»: он разбирает запрос (здесь токен просто не найден).")
    void requestWithCorrectKeyIsProcessed() {
        // токен тут левый, нам важно только то что это не 401
        ApiResponse response = Steps.logout(newToken());

        assertNotEquals(401, response.status(), "Правильный ключ не должен приводить к 401");
    }
}
