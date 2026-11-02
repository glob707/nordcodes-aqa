package ru.nordcodes.aqa.fixtures;

import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

/** Варианты неуспешных ответов внешнего сервиса для параметризованных тестов. */
public final class ExternalFailures {

    private ExternalFailures() {
    }

    /** Коды отказа (4xx) и ошибки (5xx) внешнего сервиса с пояснением для отчёта. */
    public static Stream<Arguments> errorStatuses() {
        return Stream.of(
                Arguments.of(400, "отказ: некорректный запрос"),
                Arguments.of(401, "отказ: пользователь не авторизован"),
                Arguments.of(403, "отказ: доступ запрещён"),
                Arguments.of(404, "отказ: ресурс не найден"),
                Arguments.of(500, "ошибка: внутренняя ошибка сервиса"),
                Arguments.of(502, "ошибка: ошибка шлюза"),
                Arguments.of(503, "ошибка: сервис временно недоступен"));
    }

    /** Коды успешного ответа (2xx). Тело ответа приложение не анализирует. */
    public static Stream<Arguments> successStatuses() {
        return Stream.of(
                Arguments.of(200, "OK"),
                Arguments.of(201, "Created"),
                Arguments.of(204, "No Content"));
    }

    /** Сетевые сбои, при которых внешний сервис не возвращает HTTP-ответ. */
    public static Stream<Arguments> networkFaults() {
        return Stream.of(
                Arguments.of(Fault.CONNECTION_RESET_BY_PEER, "соединение сброшено"),
                Arguments.of(Fault.EMPTY_RESPONSE, "соединение закрыто без ответа"),
                Arguments.of(Fault.RANDOM_DATA_THEN_CLOSE, "вместо HTTP-ответа пришли случайные данные"));
    }
}
