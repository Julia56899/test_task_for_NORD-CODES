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

@Epic("Сервис авторизации")
@Feature("Проверка действия (action)")
class ActionValidationTest extends BaseTest {

    @ParameterizedTest(name = "Действие «{0}» отклоняется")
    @ValueSource(strings = {"FOO", "login", "Login", "logout", "action", "DELETE", "LOGIN;LOGOUT"})
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Неизвестное действие отклоняется")
    @Description("Разрешены только LOGIN, ACTION и LOGOUT, написанные заглавными буквами и без лишних символов.")
    void unknownActionIsRejected(String action) {
        // пробелы по краям не проверяем, приложение их само обрезает
        ApiResponse response = ApiClient.send(newToken(), action);

        Steps.assertStatus(response, 400);
        Steps.assertError(response);
        Steps.assertExternalCalls(externalService, "/auth", 0);
        Steps.assertExternalCalls(externalService, "/doAction", 0);
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Запрос без действия отклоняется")
    void missingActionIsRejected() {
        // параметр action не передан
        ApiResponse response = ApiClient.send(newToken(), null);

        Steps.assertStatus(response, 400);
        Steps.assertError(response);
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Запрос с пустым действием отклоняется")
    void emptyActionIsRejected() {
        // action передан, но пустой
        ApiResponse response = ApiClient.send(newToken(), "");

        Steps.assertStatus(response, 400);
        Steps.assertError(response);
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Сообщение об ошибке перечисляет допустимые действия")
    @Description("Чтобы пользователь понял, что делать, в тексте ошибки должны быть названы все три допустимых действия.")
    void errorMessageListsAllowedActions() {
        // текст ошибки должен подсказывать, какие действия бывают
        ApiResponse response = ApiClient.send(newToken(), "FOO");

        Steps.assertError(response);
        org.junit.jupiter.api.Assertions.assertTrue(
                response.message().contains("LOGIN") && response.message().contains("ACTION")
                        && response.message().contains("LOGOUT"),
                "В сообщении должны быть LOGIN, ACTION и LOGOUT. Получено: " + response.message());
    }
}
