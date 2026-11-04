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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@Epic("Сервис авторизации")
@Feature("Проверка формата токена")
class TokenValidationTest extends BaseTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "Слишком короткий токен (31 символ)|AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "Слишком длинный токен (33 символа)|AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "Токен из строчных букв|aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "Токен со спецсимволом|AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA!",
            "Токен с пробелом внутри|AAAAAAAAAAAAAAAA AAAAAAAAAAAAAAA",
            "Токен из русских букв|АААААААААААААААААААААААААААААААА",
            "Токен с дефисом (формат UUID)|AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA"
    })
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Токен неправильного формата отклоняется")
    @Description("Токен должен состоять ровно из 32 символов A-Z0-9. Все остальное сервис обязан отклонить с ошибкой 400, "
            + "не обращаясь к внешнему сервису.")
    void invalidTokenIsRejected(String caseName, String token) {
        // в задании токен это 32 символа, все что не подходит должно упасть на проверке до похода во внешний сервис
        ApiResponse response = Steps.login(token);

        Steps.assertStatus(response, 400);
        Steps.assertError(response);
        Steps.assertExternalCalls(externalService, "/auth", 0);
    }

    @ParameterizedTest(name = "Действие {0} с пустым токеном отклоняется")
    @ValueSource(strings = {"LOGIN", "ACTION", "LOGOUT"})
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Пустой токен отклоняется для любого действия")
    void emptyTokenIsRejected(String action) {
        // пустая строка вместо токена, смотрим для всех действий
        ApiResponse response = ApiClient.send("", action);

        Steps.assertStatus(response, 400);
        Steps.assertError(response);
    }

    @ParameterizedTest(name = "Действие {0} без токена отклоняется")
    @ValueSource(strings = {"LOGIN", "ACTION", "LOGOUT"})
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Запрос без токена отклоняется для любого действия")
    void missingTokenIsRejected(String action) {
        // параметр token вообще не передаем
        ApiResponse response = ApiClient.send(null, action);

        Steps.assertStatus(response, 400);
        Steps.assertError(response);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "Токен из одних нулей|00000000000000000000000000000000",
            "Токен из одних цифр|12345678901234567890123456789012",
            "Токен из одних букв A-F|ABCDEFABCDEFABCDEFABCDEFABCDEFAB"
    })
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Корректный токен принимается")
    void validTokenIsAccepted(String caseName, String token) {
        // граничные значения: только нули, только цифры, только буквы. в конце выходим, чтобы не оставлять сессию
        ApiResponse response = Steps.login(token);
        try {
            Steps.assertSuccess(response);
        } finally {
            Steps.logout(token);
        }
    }

    @Test
    @Tag("defect")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("ДЕФЕКТ: токен с буквами G-Z принимается, как написано в документации")
    @Description("В документации сказано: токен состоит из символов A-Z и 0-9. "
            + "Фактически приложение принимает только 0-9 и A-F (шестнадцатеричные символы) "
            + "и отклоняет, например, токен из букв Z. Нужно уточнить у автора: ошибка в документации или в приложении.")
    void tokenWithLettersUpToZIsAccepted() {
        // по документации буквы до Z разрешены, а приложение пускает только до F. тест специально красный
        String token = "ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ";
        ApiResponse response = Steps.login(token);
        try {
            Steps.assertSuccess(response);
        } finally {
            Steps.logout(token);
        }
    }
}
