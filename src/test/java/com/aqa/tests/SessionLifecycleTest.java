package com.aqa.tests;

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

@Epic("Сервис авторизации")
@Feature("Сессия пользователя: вход, действие, выход")
class SessionLifecycleTest extends BaseTest {

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Полный сценарий: вход → действие → выход")
    @Description("Основной путь пользователя. Все три шага должны завершиться успешно.")
    void fullUserJourney() {
        // самый главный сценарий, если он сломан остальное уже не важно
        String token = newToken();

        Steps.assertSuccess(Steps.login(token));
        Steps.assertSuccess(Steps.action(token));
        Steps.assertSuccess(Steps.logout(token));
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Действие без предварительного входа запрещено")
    @Description("ACTION доступно только тем токенам, которые ранее прошли LOGIN. Внешний сервис при этом вызываться не должен.")
    void actionWithoutLoginIsForbidden() {
        // без входа действие делать нельзя, и до внешнего сервиса дойти оно не должно
        ApiResponse response = Steps.action(newToken());

        Steps.assertStatus(response, 403);
        Steps.assertError(response);
        Steps.assertExternalCalls(externalService, "/doAction", 0);
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Действие можно выполнять несколько раз за одну сессию")
    void actionCanBeRepeated() {
        // три раза подряд, во внешний сервис должно уйти три запроса
        String token = newToken();
        Steps.assertSuccess(Steps.login(token));

        Steps.assertSuccess(Steps.action(token));
        Steps.assertSuccess(Steps.action(token));
        Steps.assertSuccess(Steps.action(token));
        Steps.assertExternalCalls(externalService, "/doAction", 3);
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("После выхода действие снова запрещено")
    @Description("LOGOUT удаляет токен: после него ACTION должно отклоняться так же, как для незнакомого токена.")
    void actionAfterLogoutIsForbidden() {
        // после выхода токен удален, значит снова как незнакомый
        String token = newToken();
        Steps.assertSuccess(Steps.login(token));
        Steps.assertSuccess(Steps.logout(token));

        ApiResponse response = Steps.action(token);

        Steps.assertStatus(response, 403);
        Steps.assertError(response);
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Выход без входа возвращает ошибку")
    void logoutWithoutLoginIsRejected() {
        // выходить из несуществующей сессии нельзя
        ApiResponse response = Steps.logout(newToken());

        Steps.assertStatus(response, 403);
        Steps.assertError(response);
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Повторный выход возвращает ошибку")
    void secondLogoutIsRejected() {
        // второй выход подряд должен дать ошибку
        String token = newToken();
        Steps.assertSuccess(Steps.login(token));
        Steps.assertSuccess(Steps.logout(token));

        Steps.assertError(Steps.logout(token));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Повторный вход с тем же токеном отклоняется, а текущая сессия остается рабочей")
    void repeatedLoginIsRejectedAndSessionSurvives() {
        // повторный вход не должен ломать уже открытую сессию
        String token = newToken();
        Steps.assertSuccess(Steps.login(token));

        Steps.assertError(Steps.login(token));
        Steps.assertSuccess(Steps.action(token));
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("После выхода можно войти заново с тем же токеном")
    void loginAgainAfterLogout() {
        // после выхода токен свободен, можно входить снова
        String token = newToken();
        Steps.assertSuccess(Steps.login(token));
        Steps.assertSuccess(Steps.logout(token));

        Steps.assertSuccess(Steps.login(token));
        Steps.assertSuccess(Steps.action(token));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Сессии разных пользователей независимы")
    @Description("Выход одного пользователя не должен влиять на сессию другого.")
    void sessionsAreIndependent() {
        // два разных токена, выходим только одним
        String first = newToken();
        String second = newToken();
        Steps.assertSuccess(Steps.login(first));
        Steps.assertSuccess(Steps.login(second));

        Steps.assertSuccess(Steps.logout(first));

        Steps.assertError(Steps.action(first));
        Steps.assertSuccess(Steps.action(second));
    }
}
