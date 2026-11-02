package ru.nordcodes.aqa.tests.action;

import com.github.tomakehurst.wiremock.http.Fault;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.nordcodes.aqa.annotations.BasedOnDocumentation;
import ru.nordcodes.aqa.annotations.BasedOnObservedBehavior;
import ru.nordcodes.aqa.fixtures.Tokens;
import ru.nordcodes.aqa.tests.BaseApiTest;

import static ru.nordcodes.aqa.assertions.ResponseAssertions.assertError;
import static ru.nordcodes.aqa.assertions.ResponseAssertions.assertSuccess;

@Feature("Действие пользователя (ACTION)")
@DisplayName("Действие пользователя (ACTION)")
class ActionTest extends BaseApiTest {

    private static final String FAILURES = "ru.nordcodes.aqa.fixtures.ExternalFailures#";

    @Test
    @Story("Действие авторизованного пользователя")
    @Severity(SeverityLevel.BLOCKER)
    @BasedOnDocumentation
    @DisplayName("Выполнение действия для авторизованного пользователя")
    @Description("""
            Бизнес-контекст: вошедший в систему пользователь выполняет действие. \
            Приложение передаёт его во внешний сервис (/doAction).

            Ожидаемый результат: приложение отвечает «OK» и ровно один раз отправляет во внешний сервис \
            POST /doAction с токеном пользователя.""")
    void actionSucceedsForLoggedInUser() {
        String token = Tokens.random();
        givenLoggedIn(token);
        externalService.doAction().respondsWith(200);

        assertSuccess(app.action(token));
        externalService.doAction().verifyCalled(1, token);
    }

    @ParameterizedTest(name = "Внешний сервис ответил кодом {0} {1}")
    @MethodSource(FAILURES + "successStatuses")
    @Story("Действие авторизованного пользователя")
    @Severity(SeverityLevel.NORMAL)
    @BasedOnDocumentation
    @DisplayName("Любой успешный код (2xx) внешнего сервиса означает успешное действие")
    @Description("""
            Бизнес-контекст: по документации приложение смотрит только на код ответа внешнего сервиса.

            Ожидаемый результат: действие выполнено успешно.""")
    void actionSucceedsForAnySuccessfulStatus(int status, String meaning) {
        String token = Tokens.random();
        givenLoggedIn(token);
        externalService.doAction().respondsWith(status, "произвольное тело ответа");

        assertSuccess(app.action(token));
        externalService.doAction().verifyCalled(1, token);
    }

    @Test
    @Story("Действие авторизованного пользователя")
    @Severity(SeverityLevel.NORMAL)
    @BasedOnDocumentation
    @DisplayName("Каждое повторное действие передаётся во внешний сервис")
    @Description("""
            Бизнес-контекст: пользователь выполняет несколько действий подряд в одной сессии.

            Ожидаемый результат: каждое действие успешно, внешний сервис получил столько же запросов.""")
    void eachRepeatedActionReachesExternalService() {
        String token = Tokens.random();
        givenLoggedIn(token);
        externalService.doAction().respondsWith(200);

        assertSuccess(app.action(token));
        assertSuccess(app.action(token));
        assertSuccess(app.action(token));
        externalService.doAction().verifyCalled(3, token);
    }

    @Test
    @Story("Действие без входа в систему")
    @Severity(SeverityLevel.CRITICAL)
    @BasedOnDocumentation
    @DisplayName("Действие без предварительного входа запрещено")
    @Description("""
            Бизнес-контекст: по документации ACTION доступен только токенам, ранее прошедшим LOGIN.

            Ожидаемый результат: приложение отклоняет запрос (код 403 «Token ... not found» — \
            код и текст зафиксированы по фактическому поведению) и не обращается к внешнему сервису.""")
    void actionWithoutLoginIsRejected() {
        String token = Tokens.random();
        externalService.doAction().respondsWith(200);

        assertError(app.action(token), 403, "Token '" + token + "' not found");
        externalService.verifyNoRequests();
    }

    @Test
    @Story("Действие без входа в систему")
    @Severity(SeverityLevel.CRITICAL)
    @BasedOnDocumentation
    @DisplayName("Вход одного пользователя не даёт права на действие другому")
    @Description("""
            Бизнес-контекст: сессия принадлежит конкретному токену. Пользователь с другим, \
            не прошедшим вход токеном не должен получить доступ.

            Ожидаемый результат: действие с неизвестным токеном отклонено (403), \
            запрос к /doAction не отправлен.""")
    void actionWithUnknownTokenIsRejectedWhileOtherUserLoggedIn() {
        String loggedIn = Tokens.random();
        String stranger = Tokens.random();
        givenLoggedIn(loggedIn);
        externalService.doAction().respondsWith(200);

        assertError(app.action(stranger), 403, "Token '" + stranger + "' not found");
        externalService.doAction().verifyNotCalled();
    }

    @ParameterizedTest(name = "Внешний сервис ответил кодом {0} ({1})")
    @MethodSource(FAILURES + "errorStatuses")
    @Story("Отказ и ошибки внешнего сервиса")
    @Severity(SeverityLevel.CRITICAL)
    @BasedOnObservedBehavior
    @DisplayName("Действие не выполняется, если внешний сервис отказал или вернул ошибку")
    @Description("""
            Бизнес-контекст: результат действия определяет внешний сервис. Если он ответил отказом \
            или ошибкой, пользователь должен узнать, что действие не выполнено.

            Ожидаемый результат: приложение отвечает ошибкой (код 500, «Internal Server Error»). \
            Код 500 документацией не задан и зафиксирован по фактическому поведению.""")
    void actionFailsWhenExternalServiceRejects(int status, String meaning) {
        String token = Tokens.random();
        givenLoggedIn(token);
        externalService.doAction().respondsWith(status);

        assertError(app.action(token), 500, "Internal Server Error");
        externalService.doAction().verifyCalled(1, token);
    }

    @ParameterizedTest(name = "Сбой: {1}")
    @MethodSource(FAILURES + "networkFaults")
    @Story("Недоступность внешнего сервиса")
    @Severity(SeverityLevel.CRITICAL)
    @BasedOnObservedBehavior
    @DisplayName("Действие не выполняется, если внешний сервис недоступен")
    @Description("""
            Бизнес-контекст: при сетевом сбое выполнить действие невозможно.

            Ожидаемый результат: приложение отвечает ошибкой (код 500).""")
    void actionFailsWhenExternalServiceUnavailable(Fault fault, String description) {
        String token = Tokens.random();
        givenLoggedIn(token);
        externalService.doAction().failsWith(fault, description);

        assertError(app.action(token), 500, "Internal Server Error");
    }

    @Test
    @Story("Отказ и ошибки внешнего сервиса")
    @Severity(SeverityLevel.NORMAL)
    @BasedOnObservedBehavior
    @DisplayName("Ошибка внешнего сервиса не завершает сессию пользователя")
    @Description("""
            Бизнес-контекст: временная ошибка при выполнении действия не должна «выкидывать» \
            пользователя из системы.

            Ожидаемый результат: после ошибки внешнего сервиса следующее действие с тем же токеном \
            выполняется успешно без повторного входа.""")
    void sessionSurvivesExternalServiceError() {
        String token = Tokens.random();
        givenLoggedIn(token);
        externalService.doAction().respondsWith(500);
        assertError(app.action(token), 500, "Internal Server Error");

        externalService.doAction().respondsWith(200);
        assertSuccess(app.action(token));
        externalService.doAction().verifyCalled(2, token);
        externalService.auth().verifyCalled(1, token);
    }
}
