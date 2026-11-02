package ru.nordcodes.aqa.tests.api;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.nordcodes.aqa.annotations.BasedOnDocumentation;
import ru.nordcodes.aqa.annotations.BasedOnObservedBehavior;
import ru.nordcodes.aqa.config.TestConfig;
import ru.nordcodes.aqa.fixtures.Tokens;
import ru.nordcodes.aqa.models.Action;
import ru.nordcodes.aqa.tests.BaseApiTest;

import static ru.nordcodes.aqa.assertions.ResponseAssertions.assertError;
import static ru.nordcodes.aqa.assertions.ResponseAssertions.assertSuccess;
import static ru.nordcodes.aqa.client.AppClient.API_KEY_HEADER;
import static ru.nordcodes.aqa.client.AppClient.formParams;

@Feature("Доступ по API-ключу")
@DisplayName("Доступ по API-ключу")
class ApiKeyTest extends BaseApiTest {

    private static final String AUTH_ERROR = "Missing or invalid API Key";

    @Test
    @Story("Запрос без API-ключа")
    @Severity(SeverityLevel.BLOCKER)
    @BasedOnDocumentation
    @DisplayName("Запрос без API-ключа отклоняется")
    @Description("""
            Бизнес-контекст: по документации для доступа к сервису обязателен заголовок X-Api-Key.

            Ожидаемый результат: запрос отклонён (код 401 «Missing or invalid API Key» — \
            код и текст зафиксированы по фактическому поведению), вход не выполнен, \
            к внешнему сервису приложение не обращается.""")
    void requestWithoutApiKeyIsRejected() {
        String token = Tokens.random();
        externalService.auth().respondsWith(200);

        Response response = Allure.step("Отправить LOGIN без заголовка X-Api-Key", () -> app.anonymousRequest()
                .contentType(ContentType.URLENC)
                .formParams(formParams(token, Action.LOGIN.name()))
                .post());

        assertError(response, 401, AUTH_ERROR);
        externalService.verifyNoRequests();
    }

    @ParameterizedTest(name = "Ключ: {1}")
    @CsvSource(delimiter = '|', value = {
            "wrong-key  | посторонний ключ",
            "''         | пустое значение",
            "QAZWSXEDC  | верный ключ в другом регистре",
            "qazWSXed   | верный ключ без последнего символа",
            "qazWSXedc1 | верный ключ с лишним символом"})
    @Story("Неверный API-ключ")
    @Severity(SeverityLevel.CRITICAL)
    @BasedOnDocumentation
    @DisplayName("Запрос с неверным API-ключом отклоняется")
    @Description("""
            Бизнес-контекст: по документации API-ключ статический и проверяется при каждом запросе. \
            Доступ возможен только с точным значением ключа.

            Ожидаемый результат: запрос отклонён (код 401 «Missing or invalid API Key»), \
            вход не выполнен, к внешнему сервису приложение не обращается.""")
    void requestWithInvalidApiKeyIsRejected(String apiKey, String description) {
        String token = Tokens.random();
        externalService.auth().respondsWith(200);

        Response response = Allure.step("Отправить LOGIN с API-ключом «" + apiKey + "» (" + description + ")",
                () -> app.anonymousRequest()
                        .header(API_KEY_HEADER, apiKey)
                        .contentType(ContentType.URLENC)
                        .formParams(formParams(token, Action.LOGIN.name()))
                        .post());

        assertError(response, 401, AUTH_ERROR);
        externalService.verifyNoRequests();
    }

    @Test
    @Story("Корректный API-ключ")
    @Severity(SeverityLevel.NORMAL)
    @BasedOnObservedBehavior
    @DisplayName("Имя заголовка с API-ключом не зависит от регистра букв")
    @Description("""
            Бизнес-контекст: по стандарту HTTP имена заголовков нечувствительны к регистру, \
            поэтому клиент может прислать x-api-key вместо X-Api-Key.

            Ожидаемый результат: запрос принят, вход выполнен.""")
    void apiKeyHeaderNameIsCaseInsensitive() {
        String token = Tokens.random();
        externalService.auth().respondsWith(200);

        Response response = Allure.step("Отправить LOGIN с заголовком x-api-key в нижнем регистре",
                () -> app.anonymousRequest()
                        .header("x-api-key", TestConfig.apiKey())
                        .contentType(ContentType.URLENC)
                        .formParams(formParams(token, Action.LOGIN.name()))
                        .post());

        assertSuccess(response);
        externalService.auth().verifyCalled(1, token);
    }
}
