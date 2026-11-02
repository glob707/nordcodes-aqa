package ru.nordcodes.aqa.tests.logout;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.nordcodes.aqa.annotations.BasedOnDocumentation;
import ru.nordcodes.aqa.annotations.BasedOnObservedBehavior;
import ru.nordcodes.aqa.fixtures.Tokens;
import ru.nordcodes.aqa.tests.BaseApiTest;

import static ru.nordcodes.aqa.assertions.ResponseAssertions.assertError;
import static ru.nordcodes.aqa.assertions.ResponseAssertions.assertSuccess;

@Feature("Завершение сессии (LOGOUT)")
@DisplayName("Завершение сессии (LOGOUT)")
class LogoutTest extends BaseApiTest {

    @Test
    @Story("Успешное завершение сессии")
    @Severity(SeverityLevel.BLOCKER)
    @BasedOnDocumentation
    @DisplayName("Завершение сессии вошедшего пользователя")
    @Description("""
            Бизнес-контекст: пользователь выходит из системы, приложение удаляет его токен.

            Ожидаемый результат: приложение отвечает «OK». Документация не описывает обращений \
            к внешнему сервису при LOGOUT — проверяется, что их нет.""")
    void logoutSucceedsForLoggedInUser() {
        String token = Tokens.random();
        givenLoggedIn(token);

        assertSuccess(app.logout(token));
        externalService.doAction().verifyNotCalled();
        externalService.auth().verifyCalled(1, token);
    }

    @Test
    @Story("Удаление токена из хранилища")
    @Severity(SeverityLevel.CRITICAL)
    @BasedOnDocumentation
    @DisplayName("После завершения сессии действие недоступно")
    @Description("""
            Бизнес-контекст: LOGOUT удаляет токен, а ACTION доступен только вошедшим пользователям.

            Ожидаемый результат: действие после выхода отклонено (403 «Token ... not found»), \
            запрос к /doAction не отправлен.""")
    void actionAfterLogoutIsRejected() {
        String token = Tokens.random();
        givenLoggedIn(token);
        assertSuccess(app.logout(token));
        externalService.doAction().respondsWith(200);

        assertError(app.action(token), 403, "Token '" + token + "' not found");
        externalService.doAction().verifyNotCalled();
    }

    @Test
    @Story("Удаление токена из хранилища")
    @Severity(SeverityLevel.CRITICAL)
    @BasedOnDocumentation
    @DisplayName("После завершения сессии можно снова войти с тем же токеном")
    @Description("""
            Бизнес-контекст: после выхода токен удалён из хранилища, поэтому новый вход \
            не должен считаться повторным.

            Ожидаемый результат: повторный LOGIN успешен и снова подтверждается внешним сервисом, \
            после чего действие доступно.""")
    void loginAgainAfterLogoutSucceeds() {
        String token = Tokens.random();
        givenLoggedIn(token);
        assertSuccess(app.logout(token));
        externalService.doAction().respondsWith(200);

        assertSuccess(app.login(token));
        assertSuccess(app.action(token));
        externalService.auth().verifyCalled(2, token);
    }

    @Test
    @Story("Завершение несуществующей сессии")
    @Severity(SeverityLevel.NORMAL)
    @BasedOnObservedBehavior
    @DisplayName("Завершение сессии без предварительного входа отклоняется")
    @Description("""
            Бизнес-контекст: нельзя завершить сессию, которой нет. Документация этот случай не описывает.

            Ожидаемый результат (по фактическому поведению): ошибка 403 «Token ... not found», \
            обращений к внешнему сервису нет.""")
    void logoutWithoutLoginIsRejected() {
        String token = Tokens.random();

        assertError(app.logout(token), 403, "Token '" + token + "' not found");
        externalService.verifyNoRequests();
    }

    @Test
    @Story("Завершение несуществующей сессии")
    @Severity(SeverityLevel.NORMAL)
    @BasedOnObservedBehavior
    @DisplayName("Повторное завершение сессии отклоняется")
    @Description("""
            Бизнес-контекст: пользователь дважды нажимает «Выйти».

            Ожидаемый результат (по фактическому поведению): первый LOGOUT успешен, \
            второй отклонён с ошибкой 403 «Token ... not found».""")
    void repeatedLogoutIsRejected() {
        String token = Tokens.random();
        givenLoggedIn(token);
        assertSuccess(app.logout(token));

        assertError(app.logout(token), 403, "Token '" + token + "' not found");
    }

    @Test
    @Story("Несколько пользователей")
    @Severity(SeverityLevel.CRITICAL)
    @BasedOnDocumentation
    @DisplayName("Выход одного пользователя не затрагивает сессию другого")
    @Description("""
            Бизнес-контекст: в системе работают два пользователя, один из них выходит.

            Ожидаемый результат: второй пользователь продолжает выполнять действия, \
            а первый — больше не может.""")
    void logoutDoesNotAffectOtherUsers() {
        String leaving = Tokens.random();
        String staying = Tokens.random();
        givenLoggedIn(leaving);
        givenLoggedIn(staying);
        externalService.doAction().respondsWith(200);

        assertSuccess(app.logout(leaving));

        assertSuccess(app.action(staying));
        assertError(app.action(leaving), 403, "Token '" + leaving + "' not found");
        externalService.doAction().verifyCalled(1, staying);
        externalService.doAction().verifyCalled(0, leaving);
    }
}
