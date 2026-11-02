package ru.nordcodes.aqa.tests.login;

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
import ru.nordcodes.aqa.config.TestConfig;
import ru.nordcodes.aqa.fixtures.Tokens;
import ru.nordcodes.aqa.tests.BaseApiTest;

import static ru.nordcodes.aqa.assertions.ResponseAssertions.assertError;
import static ru.nordcodes.aqa.assertions.ResponseAssertions.assertSuccess;

@Feature("Вход в систему (LOGIN)")
@DisplayName("Вход в систему (LOGIN)")
class LoginTest extends BaseApiTest {

    private static final String FAILURES = "ru.nordcodes.aqa.fixtures.ExternalFailures#";

    @Test
    @Story("Успешный вход")
    @Severity(SeverityLevel.BLOCKER)
    @BasedOnDocumentation
    @DisplayName("Вход пользователя, подтверждённый внешним сервисом")
    @Description("""
            Бизнес-контекст: прежде чем выполнять действия, пользователь входит в систему. \
            Приложение подтверждает вход у внешнего сервиса (/auth) и запоминает токен.

            Ожидаемый результат: приложение отвечает «OK» и ровно один раз отправляет во внешний сервис \
            POST /auth с токеном пользователя.""")
    void loginSucceedsWhenExternalServiceConfirms() {
        String token = Tokens.random();
        externalService.auth().respondsWith(200);

        assertSuccess(app.login(token));
        externalService.auth().verifyCalled(1, token);
        externalService.doAction().verifyNotCalled();
    }

    @ParameterizedTest(name = "Внешний сервис ответил кодом {0} {1}")
    @MethodSource(FAILURES + "successStatuses")
    @Story("Успешный вход")
    @Severity(SeverityLevel.NORMAL)
    @BasedOnDocumentation
    @DisplayName("Любой успешный код (2xx) внешнего сервиса означает успешный вход")
    @Description("""
            Бизнес-контекст: по документации приложение смотрит только на код ответа внешнего сервиса. \
            Успешными в HTTP считаются все коды 2xx.

            Ожидаемый результат: вход выполнен, после чего пользователь может выполнить действие.""")
    void loginSucceedsForAnySuccessfulStatus(int status, String meaning) {
        String token = Tokens.random();
        externalService.auth().respondsWith(status);
        externalService.doAction().respondsWith(200);

        assertSuccess(app.login(token));
        assertSuccess(app.action(token));
        externalService.auth().verifyCalled(1, token);
    }

    @Test
    @Story("Успешный вход")
    @Severity(SeverityLevel.NORMAL)
    @BasedOnDocumentation
    @DisplayName("Содержимое ответа внешнего сервиса не влияет на результат входа")
    @Description("""
            Бизнес-контекст: по документации внешний сервис может вернуть любое тело ответа, \
            а приложение смотрит только на код.

            Ожидаемый результат: при коде 200 с произвольным (даже некорректным) телом вход выполнен.""")
    void loginIgnoresExternalResponseBody() {
        String token = Tokens.random();
        externalService.auth().respondsWith(200, "{ это не JSON");

        assertSuccess(app.login(token));
        externalService.auth().verifyCalled(1, token);
    }

    @ParameterizedTest(name = "Внешний сервис ответил кодом {0} ({1})")
    @MethodSource(FAILURES + "errorStatuses")
    @Story("Отказ и ошибки внешнего сервиса")
    @Severity(SeverityLevel.CRITICAL)
    @BasedOnObservedBehavior
    @DisplayName("Вход не выполняется, если внешний сервис отказал или вернул ошибку")
    @Description("""
            Бизнес-контекст: пользователь не должен попасть в систему, если внешний сервис не подтвердил вход.

            Ожидаемый результат: приложение отвечает ошибкой (код 500, «Internal Server Error»), \
            токен не сохраняется — действие от имени этого пользователя невозможно.

            Код 500 документацией не задан и зафиксирован по фактическому поведению.""")
    void loginFailsWhenExternalServiceRejects(int status, String meaning) {
        String token = Tokens.random();
        externalService.auth().respondsWith(status);

        assertError(app.login(token), 500, "Internal Server Error");
        externalService.auth().verifyCalled(1, token);
        assertUserIsNotLoggedIn(token);
    }

    @ParameterizedTest(name = "Сбой: {1}")
    @MethodSource(FAILURES + "networkFaults")
    @Story("Недоступность внешнего сервиса")
    @Severity(SeverityLevel.CRITICAL)
    @BasedOnObservedBehavior
    @DisplayName("Вход не выполняется, если внешний сервис недоступен")
    @Description("""
            Бизнес-контекст: при сетевом сбое подтвердить вход невозможно.

            Ожидаемый результат: приложение отвечает ошибкой (код 500), токен не сохраняется.""")
    void loginFailsWhenExternalServiceUnavailable(Fault fault, String description) {
        String token = Tokens.random();
        externalService.auth().failsWith(fault, description);

        assertError(app.login(token), 500, "Internal Server Error");
        assertUserIsNotLoggedIn(token);
    }

    @Test
    @Story("Недоступность внешнего сервиса")
    @Severity(SeverityLevel.NORMAL)
    @BasedOnObservedBehavior
    @DisplayName("Вход не выполняется, если внешний сервис не ответил вовремя")
    @Description("""
            Бизнес-контекст: приложение ждёт ответ внешнего сервиса не дольше 10 секунд \
            (значение найдено в настройках приложения, в документации не указано).

            Ожидаемый результат: если ответ задерживается сверх этого времени, приложение отвечает \
            ошибкой (код 500), токен не сохраняется.

            Тест выполняется около 10 секунд.""")
    void loginFailsOnExternalServiceTimeout() {
        String token = Tokens.random();
        externalService.auth().respondsAfter(TestConfig.externalReadTimeout().plusSeconds(2));

        assertError(app.login(token), 500, "Internal Server Error");
        assertUserIsNotLoggedIn(token);
    }

    @Test
    @Story("Отказ и ошибки внешнего сервиса")
    @Severity(SeverityLevel.NORMAL)
    @BasedOnObservedBehavior
    @DisplayName("После отказа внешнего сервиса вход можно повторить")
    @Description("""
            Бизнес-контекст: неудачная попытка входа не должна блокировать пользователя.

            Ожидаемый результат: после ответа внешнего сервиса с ошибкой повторный LOGIN \
            с тем же токеном успешен, а внешний сервис получил оба запроса.""")
    void loginCanBeRetriedAfterRejection() {
        String token = Tokens.random();
        externalService.auth().respondsWith(503);
        assertError(app.login(token), 500, "Internal Server Error");

        externalService.auth().respondsWith(200);
        assertSuccess(app.login(token));
        externalService.auth().verifyCalled(2, token);
    }

    @Test
    @Story("Повторный вход")
    @Severity(SeverityLevel.CRITICAL)
    @BasedOnObservedBehavior
    @DisplayName("Повторный вход с тем же токеном отклоняется")
    @Description("""
            Бизнес-контекст: пользователь, уже вошедший в систему, пытается войти ещё раз.

            Ожидаемый результат: приложение отвечает ошибкой 409 «Token ... already exists» \
            и не обращается к внешнему сервису повторно. Код и текст документацией не заданы.""")
    void repeatedLoginWithSameTokenIsRejected() {
        String token = Tokens.random();
        givenLoggedIn(token);

        assertError(app.login(token), 409, "Token '" + token + "' already exists");
        externalService.auth().verifyCalled(1, token);
    }

    @Test
    @Story("Повторный вход")
    @Severity(SeverityLevel.NORMAL)
    @BasedOnDocumentation
    @DisplayName("Вход второго пользователя с другим токеном")
    @Description("""
            Бизнес-контекст: в системе одновременно работают несколько пользователей.

            Ожидаемый результат: вход с другим токеном успешен, каждый вход подтверждён \
            отдельным запросом к внешнему сервису с собственным токеном.""")
    void loginWithAnotherTokenSucceeds() {
        String first = Tokens.random();
        String second = Tokens.random();
        externalService.auth().respondsWith(200);

        assertSuccess(app.login(first));
        assertSuccess(app.login(second));
        externalService.auth().verifyCalled(1, first);
        externalService.auth().verifyCalled(1, second);
    }

    private void assertUserIsNotLoggedIn(String token) {
        externalService.doAction().respondsWith(200);
        assertError(app.action(token), 403, "Token '" + token + "' not found");
        externalService.doAction().verifyNotCalled();
    }
}
