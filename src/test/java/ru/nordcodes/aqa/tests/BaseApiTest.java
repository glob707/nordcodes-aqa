package ru.nordcodes.aqa.tests;

import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.nordcodes.aqa.client.AppClient;
import ru.nordcodes.aqa.environment.TestEnvironmentExtension;
import ru.nordcodes.aqa.mock.ExternalServiceMock;

import static ru.nordcodes.aqa.assertions.ResponseAssertions.assertSuccess;

/**
 * Общая основа тестов: окружение (приложение + WireMock), клиент приложения и mock внешнего сервиса.
 */
@Epic("Сервис управления сессиями пользователей")
@ExtendWith(TestEnvironmentExtension.class)
public abstract class BaseApiTest {

    protected AppClient app;
    protected ExternalServiceMock externalService;

    @BeforeEach
    void setUpClients(AppClient app, ExternalServiceMock externalService) {
        this.app = app;
        this.externalService = externalService;
    }

    /** Предусловие: пользователь с указанным токеном успешно вошёл в систему. */
    protected void givenLoggedIn(String token) {
        Allure.step("Предусловие: пользователь с токеном " + token + " вошёл в систему", () -> {
            externalService.auth().respondsWith(200);
            assertSuccess(app.login(token));
        });
    }
}
