package ru.nordcodes.aqa.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.qameta.allure.Allure;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Внешний сервис, к которому обращается приложение. Имитируется WireMock.
 * Перед каждым тестом WireMock полностью сбрасывается, поэтому ответы внешнего сервиса
 * тест настраивает явно.
 */
public final class ExternalServiceMock {

    private final WireMockServer wireMock;
    private final ExternalEndpointMock auth;
    private final ExternalEndpointMock doAction;

    public ExternalServiceMock(WireMockServer wireMock) {
        this.wireMock = wireMock;
        this.auth = new ExternalEndpointMock(wireMock, "/auth");
        this.doAction = new ExternalEndpointMock(wireMock, "/doAction");
    }

    /** Эндпоинт проверки входа пользователя. */
    public ExternalEndpointMock auth() {
        return auth;
    }

    /** Эндпоинт выполнения действия пользователя. */
    public ExternalEndpointMock doAction() {
        return doAction;
    }

    public void verifyNoRequests() {
        Allure.step("Проверить: приложение не обращалось к внешнему сервису", () -> {
            List<LoggedRequest> all = wireMock.findAll(anyRequestedFor(anyUrl()));
            assertEquals(0, all.size(), "Приложение не должно обращаться к внешнему сервису, но обращения были: "
                    + ExternalEndpointMock.describe(all));
        });
    }
}
