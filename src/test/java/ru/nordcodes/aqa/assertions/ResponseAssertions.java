package ru.nordcodes.aqa.assertions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import ru.nordcodes.aqa.models.ApiResponse;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Проверки ответа приложения: HTTP-код, заголовок Content-Type и JSON-тело.
 */
public final class ResponseAssertions {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ResponseAssertions() {
    }

    /** Операция выполнена: 200 и {@code {"result":"OK"}} без поля message. */
    public static void assertSuccess(Response response) {
        Allure.step("Проверить: операция выполнена успешно (код 200, result = OK)", () -> {
            ApiResponse body = parse(response);
            assertAll("Успешный ответ приложения",
                    () -> assertEquals(200, response.statusCode(), "HTTP-код ответа"),
                    () -> assertJsonContentType(response),
                    () -> assertEquals(ApiResponse.OK, body.result(), "Поле result"),
                    () -> assertNull(body.message(), "В успешном ответе не должно быть поля message"));
        });
    }

    /**
     * Запрос отклонён: ожидаемый HTTP-код, {@code result = ERROR}
     * и сообщение об ошибке, содержащее {@code expectedMessagePart}.
     */
    public static void assertError(Response response, int expectedStatus, String expectedMessagePart) {
        Allure.step("Проверить: запрос отклонён (код " + expectedStatus + ", result = ERROR, в сообщении «"
                + expectedMessagePart + "»)", () -> {
            ApiResponse body = parse(response);
            assertAll("Ответ приложения с ошибкой",
                    () -> assertEquals(expectedStatus, response.statusCode(), "HTTP-код ответа"),
                    () -> assertJsonContentType(response),
                    () -> assertEquals(ApiResponse.ERROR, body.result(), "Поле result"),
                    () -> {
                        assertNotNull(body.message(), "В ответе с ошибкой должно быть поле message");
                        assertTrue(body.message().contains(expectedMessagePart),
                                "Сообщение об ошибке должно содержать «" + expectedMessagePart
                                        + "», фактически: «" + body.message() + "»");
                    });
        });
    }

    /** Запрос отклонён: ожидаемый HTTP-код и JSON-тело ошибки с непустым сообщением. */
    public static void assertError(Response response, int expectedStatus) {
        Allure.step("Проверить: запрос отклонён (код " + expectedStatus + ", result = ERROR)", () -> {
            ApiResponse body = parse(response);
            assertAll("Ответ приложения с ошибкой",
                    () -> assertEquals(expectedStatus, response.statusCode(), "HTTP-код ответа"),
                    () -> assertJsonContentType(response),
                    () -> assertEquals(ApiResponse.ERROR, body.result(), "Поле result"),
                    () -> assertNotNull(body.message(), "В ответе с ошибкой должно быть поле message"));
        });
    }

    private static void assertJsonContentType(Response response) {
        String contentType = response.contentType();
        assertTrue(contentType != null && contentType.startsWith("application/json"),
                "Заголовок Content-Type должен быть application/json, фактически: " + contentType);
    }

    private static ApiResponse parse(Response response) {
        String raw = response.asString();
        try {
            return MAPPER.readValue(raw, ApiResponse.class);
        } catch (JsonProcessingException e) {
            return fail("Тело ответа — не JSON нужного формата. Код " + response.statusCode()
                    + ", тело: «" + raw + "»");
        }
    }
}
