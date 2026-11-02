package ru.nordcodes.aqa.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.qameta.allure.Allure;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Один эндпоинт внешнего сервиса: настройка ответа и проверка обращений к нему.
 */
public final class ExternalEndpointMock {

    private final WireMockServer wireMock;
    private final String path;

    ExternalEndpointMock(WireMockServer wireMock, String path) {
        this.wireMock = wireMock;
        this.path = path;
    }

    public void respondsWith(int status) {
        respondsWith(status, "");
    }

    public void respondsWith(int status, String body) {
        Allure.step("Внешний сервис отвечает на " + path + " кодом " + status,
                () -> stub(aResponse().withStatus(status).withBody(body)));
    }

    /** Сетевой сбой: соединение обрывается или ответ не приходит в корректном виде. */
    public void failsWith(Fault fault, String description) {
        Allure.step("Внешний сервис недоступен на " + path + ": " + description,
                () -> stub(aResponse().withFault(fault)));
    }

    public void respondsAfter(Duration delay) {
        Allure.step("Внешний сервис отвечает на " + path + " только через " + delay.toSeconds() + " с",
                () -> stub(aResponse().withStatus(200).withFixedDelay((int) delay.toMillis())));
    }

    /**
     * Проверяет, что к эндпоинту было ровно {@code times} обращений с данным токеном
     * и каждое обращение к эндпоинту — POST с заголовками из документации и form-параметром {@code token}.
     */
    public void verifyCalled(int times, String token) {
        Allure.step("Проверить: приложение обратилось к " + path + " " + times + " раз(а) с токеном " + token, () -> {
            List<LoggedRequest> all = wireMock.findAll(anyRequestedFor(urlPathEqualTo(path)));
            List<LoggedRequest> withToken = wireMock.findAll(anyRequestedFor(urlPathEqualTo(path))
                    .withFormParam("token", equalTo(token)));
            assertEquals(times, withToken.size(), "Неверное количество обращений к " + path
                    + " с токеном " + token + ". Фактические обращения: " + describe(all));

            List<LoggedRequest> wellFormed = wireMock.findAll(postRequestedFor(urlPathEqualTo(path))
                    .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                    .withHeader("Accept", equalTo("application/json"))
                    .withFormParam("token", matching("[0-9A-Z]{32}")));
            assertEquals(all.size(), wellFormed.size(), "Обращения к " + path + " должны быть POST-запросами "
                    + "с Content-Type application/x-www-form-urlencoded, Accept application/json и телом token=<токен>. "
                    + "Фактические обращения: " + describe(all));
        });
    }

    public void verifyNotCalled() {
        Allure.step("Проверить: приложение не обращалось к " + path, () -> {
            List<LoggedRequest> all = wireMock.findAll(anyRequestedFor(urlPathEqualTo(path)));
            assertEquals(0, all.size(), "Обращений к " + path + " быть не должно, но они были: " + describe(all));
        });
    }

    private void stub(ResponseDefinitionBuilder response) {
        wireMock.stubFor(post(urlPathEqualTo(path)).willReturn(response));
    }

    static String describe(List<LoggedRequest> requests) {
        if (requests.isEmpty()) {
            return "нет";
        }
        return requests.stream()
                .map(r -> r.getMethod() + " " + r.getUrl()
                        + " [Content-Type: " + r.getHeader("Content-Type")
                        + ", Accept: " + r.getHeader("Accept") + "] " + r.getBodyAsString())
                .collect(Collectors.joining("; "));
    }
}
