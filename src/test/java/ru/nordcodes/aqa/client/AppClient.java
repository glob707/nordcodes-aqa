package ru.nordcodes.aqa.client;

import io.qameta.allure.Allure;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.EncoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import ru.nordcodes.aqa.config.TestConfig;
import ru.nordcodes.aqa.models.Action;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * HTTP-клиент тестируемого приложения (единственный эндпоинт {@code POST /endpoint}).
 * Каждый вызов попадает в отчёт Allure отдельным шагом с приложенными запросом и ответом.
 */
public final class AppClient {

    public static final String ENDPOINT = "/endpoint";
    public static final String API_KEY_HEADER = "X-Api-Key";

    private final RequestSpecification baseSpec;

    public AppClient(String baseUrl) {
        this.baseSpec = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setBasePath(ENDPOINT)
                .setAccept(ContentType.JSON)
                // Отправляем Content-Type ровно как в документации, без добавления charset
                .setConfig(RestAssuredConfig.config().encoderConfig(
                        EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                .addFilter(new AllureRestAssured()
                        .setRequestAttachmentName("Запрос к приложению")
                        .setResponseAttachmentName("Ответ приложения"))
                .build();
    }

    public Response login(String token) {
        return perform(Action.LOGIN, token, "Пользователь входит в систему (LOGIN)");
    }

    public Response action(String token) {
        return perform(Action.ACTION, token, "Пользователь выполняет действие (ACTION)");
    }

    public Response logout(String token) {
        return perform(Action.LOGOUT, token, "Пользователь завершает сессию (LOGOUT)");
    }

    /** Корректно оформленный запрос с произвольным набором form-параметров. */
    public Response send(String stepName, Map<String, String> formParams) {
        return Allure.step(stepName, () -> authorizedRequest()
                .contentType(ContentType.URLENC)
                .formParams(formParams)
                .post());
    }

    /** Запрос с корректным API-ключом — для сценариев, где проверяется сам формат HTTP-запроса. */
    public RequestSpecification authorizedRequest() {
        return anonymousRequest().header(API_KEY_HEADER, TestConfig.apiKey());
    }

    /** Запрос без API-ключа. */
    public RequestSpecification anonymousRequest() {
        return given().spec(baseSpec);
    }

    public static Map<String, String> formParams(String token, String action) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("token", token);
        params.put("action", action);
        return params;
    }

    private Response perform(Action action, String token, String stepName) {
        return send(stepName + ", токен " + token, formParams(token, action.name()));
    }
}
