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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

@Epic("Сервис авторизации")
@Feature("Взаимодействие с внешним сервисом")
class ExternalServiceTest extends BaseTest {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Вход обращается к внешнему сервису /auth с токеном пользователя")
    @Description("Проверяем, что именно уходит во внешний сервис: адрес, форма, заголовки и токен.")
    void loginCallsAuthWithToken() {
        // смотрим что именно приложение отправило в WireMock: адрес, тело и заголовки
        String token = newToken();

        Steps.assertSuccess(Steps.login(token));

        externalService.verify(1, Steps.externalRequest("/auth")
                .withRequestBody(equalTo("token=" + token))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .withHeader("Accept", containing("application/json")));
        Steps.assertExternalCalls(externalService, "/doAction", 0);
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Действие обращается к внешнему сервису /doAction с токеном пользователя")
    void actionCallsDoActionWithToken() {
        // то же самое для действия, и заодно что /auth второй раз не дергали
        String token = newToken();
        Steps.assertSuccess(Steps.login(token));

        Steps.assertSuccess(Steps.action(token));

        externalService.verify(1, Steps.externalRequest("/doAction")
                .withRequestBody(equalTo("token=" + token))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .withHeader("Accept", containing("application/json")));
        Steps.assertExternalCalls(externalService, "/auth", 1);
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Выход не обращается к внешнему сервису")
    void logoutDoesNotCallExternalService() {
        // при выходе во внешний сервис ходить не нужно
        String token = newToken();
        Steps.assertSuccess(Steps.login(token));

        Steps.assertSuccess(Steps.logout(token));

        Steps.assertExternalCalls(externalService, "/auth", 1);
        Steps.assertExternalCalls(externalService, "/doAction", 0);
    }

    @ParameterizedTest(name = "Внешний сервис ответил {0}")
    @ValueSource(ints = {400, 401, 403, 404, 500, 502, 503})
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Если внешний сервис отказал во входе, пользователь не входит")
    @Description("Токен сохраняется только при успешной проверке. Если внешний сервис вернул ошибку, "
            + "пользователь получает ошибку, а действие после этого остается запрещенным.")
    void failedAuthDoesNotCreateSession(int externalStatus) {
        // внешний сервис говорит нет, значит токен не сохраняем и действие потом должно быть запрещено
        stubExternal("/auth", externalStatus);
        String token = newToken();

        ApiResponse login = Steps.login(token);

        Steps.assertError(login);
        ApiResponse action = Steps.action(token);
        Steps.assertStatus(action, 403);
        Steps.assertExternalCalls(externalService, "/doAction", 0);
    }

    @ParameterizedTest(name = "Внешний сервис ответил {0}")
    @ValueSource(ints = {200, 201, 204})
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Любой успешный (2xx) ответ внешнего сервиса означает успешный вход")
    void successfulAuthCodesAreAccepted(int externalStatus) {
        // приложение смотрит только на код, любой 2xx это успех
        stubExternal("/auth", externalStatus);

        Steps.assertSuccess(Steps.login(newToken()));
    }

    @ParameterizedTest(name = "Внешний сервис ответил {0}")
    @ValueSource(ints = {400, 403, 404, 500, 503})
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Если внешний сервис не выполнил действие, пользователь получает ошибку")
    void failedActionReturnsError(int externalStatus) {
        // сначала нормально входим, потом внешний сервис ломается на действии
        String token = newToken();
        Steps.assertSuccess(Steps.login(token));
        stubExternal("/doAction", externalStatus);

        ApiResponse response = Steps.action(token);

        Steps.assertError(response);
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Медленный внешний сервис (ответ через 2 секунды) не ломает вход")
    void slowExternalServiceStillWorks() {
        // задержка 2 секунды, это еще не повод падать
        externalService.stubFor(post(urlEqualTo("/auth")).willReturn(aResponse().withStatus(200).withFixedDelay(2000)));

        Steps.assertSuccess(Steps.login(newToken()));
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Тело ответа внешнего сервиса не важно: учитывается только код")
    void externalResponseBodyIsIgnored() {
        // по документации тело ответа не читается, отдаем что попало
        externalService.stubFor(post(urlEqualTo("/auth"))
                .willReturn(aResponse().withStatus(200).withBody("{\"совершенно\":\"другое тело\"}")));

        Steps.assertSuccess(ApiClient.send(newToken(), "LOGIN"));
    }
}
