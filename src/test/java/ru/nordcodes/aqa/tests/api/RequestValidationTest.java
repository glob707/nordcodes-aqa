package ru.nordcodes.aqa.tests.api;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.nordcodes.aqa.annotations.BasedOnDocumentation;
import ru.nordcodes.aqa.annotations.KnownDefect;
import ru.nordcodes.aqa.fixtures.Tokens;
import ru.nordcodes.aqa.models.Action;
import ru.nordcodes.aqa.tests.BaseApiTest;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static ru.nordcodes.aqa.assertions.ResponseAssertions.assertError;
import static ru.nordcodes.aqa.assertions.ResponseAssertions.assertSuccess;
import static ru.nordcodes.aqa.client.AppClient.formParams;

/**
 * Документация задаёт формат параметров token и action, но не задаёт код ответа при их нарушении.
 * Код 400 зафиксирован по фактическому поведению. Текст сообщения об ошибке проверяется только
 * на упоминание параметра: остальная часть сообщения зависит от языка операционной системы сервера.
 */
@Feature("Проверка входных данных")
@DisplayName("Проверка входных данных")
class RequestValidationTest extends BaseApiTest {

    @ParameterizedTest(name = "Токен из символов: {0}")
    @MethodSource("hexTokens")
    @Story("Корректный token")
    @Severity(SeverityLevel.CRITICAL)
    @BasedOnDocumentation
    @DisplayName("Токен из 32 цифр и букв A–F принимается")
    @Description("""
            Бизнес-контекст: по документации token — строка из 32 символов A–Z и 0–9. \
            Проверяются токены из подмножества этих символов, которое приложение гарантированно принимает.

            Ожидаемый результат: вход выполнен, внешний сервис получил запрос /auth.""")
    void hexTokenIsAccepted(String description, String token) {
        assertLoginSucceeds(token);
    }

    static Stream<Arguments> hexTokens() {
        return Stream.of(
                Arguments.of("только цифры", Tokens.random("0123456789")),
                Arguments.of("только буквы A–F", Tokens.random("ABCDEF")));
    }

    @ParameterizedTest(name = "Токен из символов: {0}")
    @MethodSource("tokensWithLettersAfterF")
    @Story("Корректный token")
    @Severity(SeverityLevel.CRITICAL)
    @BasedOnDocumentation
    @KnownDefect("Дефект D-01")
    @DisplayName("Токен с буквами G–Z, допустимый по документации, принимается")
    @Description("""
            Бизнес-контекст: по документации token — строка из 32 символов A–Z и 0–9. \
            Пользователь с таким токеном должен иметь возможность войти в систему.

            Ожидаемый результат: вход выполнен, внешний сервис получил запрос /auth.

            Фактически приложение принимает только буквы A–F (шестнадцатеричные символы) \
            и отклоняет токены с буквами G–Z кодом 400 (дефект D-01).""")
    void tokenWithLettersAfterFIsAccepted(String description, String token) {
        assertLoginSucceeds(token);
    }

    static Stream<Arguments> tokensWithLettersAfterF() {
        return Stream.of(
                Arguments.of("только буквы G–Z", Tokens.random("GHIJKLMNOPQRSTUVWXYZ")),
                Arguments.of("буквы A–Z и цифры", Tokens.random().substring(0, 26) + "GHXYZ9"));
    }

    @ParameterizedTest(name = "Длина токена {0}")
    @ValueSource(ints = {1, 31, 33, 64})
    @Story("Некорректный token")
    @Severity(SeverityLevel.CRITICAL)
    @BasedOnDocumentation
    @DisplayName("Токен с длиной, отличной от 32 символов, отклоняется")
    @Description("""
            Бизнес-контекст: по документации token состоит ровно из 32 символов. \
            Проверяются граничные значения 31 и 33, а также заведомо короткий и длинный токен.

            Ожидаемый результат: запрос отклонён (код 400, сообщение указывает на token), \
            к внешнему сервису приложение не обращается.""")
    void tokenWithWrongLengthIsRejected(int length) {
        assertInvalidTokenRejected(Tokens.ofLength(length));
    }

    @ParameterizedTest(name = "Токен содержит {0}")
    @MethodSource("tokensWithInvalidCharacters")
    @Story("Некорректный token")
    @Severity(SeverityLevel.CRITICAL)
    @BasedOnDocumentation
    @DisplayName("Токен с недопустимыми символами отклоняется")
    @Description("""
            Бизнес-контекст: по документации token состоит только из заглавных латинских букв и цифр.

            Ожидаемый результат: запрос отклонён (код 400, сообщение указывает на token), \
            к внешнему сервису приложение не обращается.""")
    void tokenWithInvalidCharactersIsRejected(String description, String token) {
        assertInvalidTokenRejected(token);
    }

    static Stream<Arguments> tokensWithInvalidCharacters() {
        return Stream.of(
                Arguments.of("строчные буквы", Tokens.random().toLowerCase(Locale.ROOT)),
                Arguments.of("спецсимвол «!»", Tokens.withLastChar('!')),
                Arguments.of("дефис", Tokens.withLastChar('-')),
                Arguments.of("пробел", Tokens.withLastChar(' ')),
                Arguments.of("кириллическую букву «А»", Tokens.withLastChar('А')));
    }

    @Test
    @Story("Некорректный token")
    @Severity(SeverityLevel.CRITICAL)
    @BasedOnDocumentation
    @DisplayName("Пустой токен отклоняется")
    @Description("""
            Бизнес-контекст: token — обязательная строка из 32 символов.

            Ожидаемый результат: запрос с пустым значением token отклонён (код 400).""")
    void emptyTokenIsRejected() {
        assertInvalidTokenRejected("");
    }

    @Test
    @Story("Некорректный token")
    @Severity(SeverityLevel.CRITICAL)
    @BasedOnDocumentation
    @DisplayName("Запрос без параметра token отклоняется")
    @Description("""
            Бизнес-контекст: token — обязательный параметр запроса.

            Ожидаемый результат: запрос без token отклонён (код 400, сообщение указывает на token), \
            к внешнему сервису приложение не обращается.""")
    void missingTokenIsRejected() {
        assertRejected("Отправить LOGIN без параметра token",
                Map.of("action", Action.LOGIN.name()), "token");
    }

    @ParameterizedTest(name = "action = «{0}»")
    @ValueSource(strings = {"DELETE", "login", "Login", ""})
    @Story("Некорректный action")
    @Severity(SeverityLevel.CRITICAL)
    @BasedOnDocumentation
    @DisplayName("Неизвестное, пустое или записанное в другом регистре действие отклоняется")
    @Description("""
            Бизнес-контекст: по документации действий всего три — LOGIN, ACTION и LOGOUT.

            Ожидаемый результат: запрос с другим значением action отклонён (код 400, сообщение \
            указывает на action), к внешнему сервису приложение не обращается.

            Отказ для значений в нижнем регистре (login) — фактическое поведение: \
            документация регистр явно не оговаривает.""")
    void unknownActionIsRejected(String action) {
        assertRejected("Отправить запрос с action = «" + action + "»",
                formParams(Tokens.random(), action), "action");
    }

    @Test
    @Story("Некорректный action")
    @Severity(SeverityLevel.CRITICAL)
    @BasedOnDocumentation
    @DisplayName("Запрос без параметра action отклоняется")
    @Description("""
            Бизнес-контекст: action — обязательный параметр, определяющий операцию.

            Ожидаемый результат: запрос без action отклонён (код 400, сообщение указывает на action).""")
    void missingActionIsRejected() {
        assertRejected("Отправить запрос без параметра action",
                Map.of("token", Tokens.random()), "action");
    }

    @Test
    @Story("Некорректный формат тела запроса")
    @Severity(SeverityLevel.NORMAL)
    @BasedOnDocumentation
    @DisplayName("Запрос с пустым телом отклоняется")
    @Description("""
            Бизнес-контекст: оба параметра обязательны.

            Ожидаемый результат: запрос без параметров отклонён (код 400), \
            к внешнему сервису приложение не обращается.""")
    void emptyBodyIsRejected() {
        assertRejected("Отправить запрос без параметров", Map.of(), "token");
    }

    private void assertLoginSucceeds(String token) {
        externalService.auth().respondsWith(200);

        assertSuccess(app.login(token));
        externalService.auth().verifyCalled(1, token);
    }

    private void assertInvalidTokenRejected(String token) {
        assertRejected("Отправить LOGIN с токеном «" + token + "»",
                formParams(token, Action.LOGIN.name()), "token");
    }

    private void assertRejected(String stepName, Map<String, String> params, String invalidParam) {
        externalService.auth().respondsWith(200);

        assertError(app.send(stepName, params), 400, invalidParam);
        externalService.verifyNoRequests();
    }
}
