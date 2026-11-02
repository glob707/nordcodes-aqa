package ru.nordcodes.aqa.tests.api;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.http.Method;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import ru.nordcodes.aqa.annotations.KnownDefect;
import ru.nordcodes.aqa.fixtures.Tokens;
import ru.nordcodes.aqa.models.Action;
import ru.nordcodes.aqa.tests.BaseApiTest;

import static ru.nordcodes.aqa.assertions.ResponseAssertions.assertError;

/**
 * Документация задаёт формат запроса (POST, form-urlencoded), но не описывает реакцию на его нарушение.
 * Ожидаемые коды взяты из стандарта HTTP (RFC 9110): ошибка клиента не должна выглядеть как сбой сервера.
 */
@Feature("Формат HTTP-запроса")
@DisplayName("Формат HTTP-запроса")
class HttpProtocolTest extends BaseApiTest {

    @ParameterizedTest(name = "Метод {0}")
    @EnumSource(value = Method.class, names = {"GET", "PUT", "PATCH", "DELETE"})
    @Story("Неподдерживаемый HTTP-метод")
    @Severity(SeverityLevel.NORMAL)
    @KnownDefect("Дефект D-02")
    @DisplayName("Запрос неподдерживаемым методом отклоняется как ошибка клиента")
    @Description("""
            Бизнес-контекст: сервис принимает только POST-запросы. Клиент, отправивший запрос \
            другим методом, должен получить понятный ответ, что метод не поддерживается.

            Ожидаемый результат (стандарт HTTP): код 405 Method Not Allowed с JSON-телом ошибки, \
            вход не выполнен.

            Фактически приложение отвечает 500 «Internal Server Error», \
            как будто произошёл сбой на сервере (дефект D-02).""")
    void unsupportedMethodIsRejected(Method method) {
        String token = Tokens.random();
        externalService.auth().respondsWith(200);

        Response response = Allure.step("Отправить LOGIN методом " + method + " вместо POST",
                () -> app.authorizedRequest()
                        .queryParam("token", token)
                        .queryParam("action", Action.LOGIN.name())
                        .request(method));

        externalService.auth().verifyNotCalled();
        assertError(response, 405);
    }

    @ParameterizedTest(name = "Content-Type: {0}")
    @CsvSource(delimiter = '|', value = {
            "application/json | {\"token\":\"%s\",\"action\":\"LOGIN\"}",
            "text/plain       | token=%s&action=LOGIN"})
    @Story("Неподдерживаемый формат тела запроса")
    @Severity(SeverityLevel.NORMAL)
    @KnownDefect("Дефект D-03")
    @DisplayName("Запрос с неподдерживаемым Content-Type отклоняется как ошибка клиента")
    @Description("""
            Бизнес-контекст: по документации тело запроса передаётся в формате \
            application/x-www-form-urlencoded. Клиент, приславший данные в другом формате, \
            должен получить понятный ответ об ошибке формата.

            Ожидаемый результат (стандарт HTTP): код 415 Unsupported Media Type с JSON-телом ошибки, \
            вход не выполнен.

            Фактически приложение отвечает 500 «Internal Server Error» (дефект D-03).""")
    void unsupportedContentTypeIsRejected(String contentType, String bodyTemplate) {
        String token = Tokens.random();
        externalService.auth().respondsWith(200);

        Response response = Allure.step("Отправить LOGIN с Content-Type " + contentType,
                () -> app.authorizedRequest()
                        .contentType(contentType)
                        .body(bodyTemplate.formatted(token))
                        .post());

        externalService.auth().verifyNotCalled();
        assertError(response, 415);
    }
}
