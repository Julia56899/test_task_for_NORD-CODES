package com.aqa.tests;

import com.aqa.support.ApiClient;
import com.aqa.support.ApiResponse;
import com.aqa.support.BaseTest;
import com.aqa.support.Steps;
import com.aqa.support.TestEnvironment;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Epic("Сервис авторизации")
@Feature("Формат запросов и ответов")
class ProtocolTest extends BaseTest {

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Ответ приходит в формате JSON - и при успехе, и при ошибке")
    void responsesAreJson() {
        // смотрим заголовок Content-Type и у успешного ответа, и у ошибки
        ApiResponse success = Steps.login(newToken());
        ApiResponse error = Steps.action(newToken());

        assertTrue(success.contentType().contains("application/json"), "Тип ответа при успехе: " + success.contentType());
        assertTrue(error.contentType().contains("application/json"), "Тип ответа при ошибке: " + error.contentType());
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Порядок параметров в запросе не важен")
    void parameterOrderDoesNotMatter() {
        // сначала action, потом token
        ApiResponse response = ApiClient.post("action=LOGIN&token=" + newToken(),
                "application/x-www-form-urlencoded", TestEnvironment.API_KEY);

        Steps.assertSuccess(response);
    }

    @Test
    @Tag("defect")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("ДЕФЕКТ: GET-запрос на endpoint отклоняется как «метод не поддерживается» (405)")
    @Description("Endpoint принимает только POST. На GET правильный ответ - 405. Фактически приложение отвечает 500 "
            + "«Internal Server Error», то есть сообщает о собственной поломке, хотя ошибка в запросе клиента.")
    void getRequestIsMethodNotAllowed() {
        // тест красный: приложение отвечает 500 вместо 405
        ApiResponse response = ApiClient.get();

        assertEquals(405, response.status(), "HTTP-код ответа. Тело: " + response.body());
    }

    @Test
    @Tag("defect")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("ДЕФЕКТ: тело запроса в формате JSON отклоняется как ошибка клиента (4xx)")
    @Description("Endpoint принимает только форму (application/x-www-form-urlencoded). На JSON правильный ответ - 415 "
            + "или 400. Фактически приложение отвечает 500.")
    void jsonBodyIsClientError() {
        // тест красный: приложение отвечает 500, а должна быть ошибка клиента
        ApiResponse response = ApiClient.post("{\"token\":\"" + newToken() + "\",\"action\":\"LOGIN\"}",
                "application/json", TestEnvironment.API_KEY);

        assertEquals(4, response.status() / 100, "Ожидался код 4xx, получен " + response.status() + ". Тело: " + response.body());
    }

    @Test
    @Tag("defect")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("ДЕФЕКТ: обращение к несуществующему адресу возвращает «не найдено» (404)")
    @Description("У приложения один адрес - /endpoint. Для любого другого ожидается 404. Фактически приходит 500.")
    void unknownPathIsNotFound() {
        // тест красный: приложение отвечает 500 вместо 404
        ApiResponse response = ApiClient.postTo("/other", "a=1");

        assertEquals(404, response.status(), "HTTP-код ответа. Тело: " + response.body());
    }
}
