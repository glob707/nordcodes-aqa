package ru.nordcodes.aqa.environment;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import ru.nordcodes.aqa.client.AppClient;
import ru.nordcodes.aqa.mock.ExternalServiceMock;

/**
 * Поднимает окружение один раз на весь прогон (хранится в корневом Store JUnit
 * и закрывается после последнего теста), перед каждым тестом сбрасывает WireMock
 * и передаёт тестам клиент приложения и mock внешнего сервиса.
 */
public final class TestEnvironmentExtension implements BeforeEachCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(TestEnvironmentExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) {
        // Тесты не должны видеть заглушки и журнал запросов предыдущих тестов
        environment(context).wireMock().resetAll();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return type == AppClient.class || type == ExternalServiceMock.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        TestEnvironment environment = environment(extensionContext);
        if (parameterContext.getParameter().getType() == AppClient.class) {
            return new AppClient(environment.appUrl());
        }
        return new ExternalServiceMock(environment.wireMock());
    }

    private static TestEnvironment environment(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE)
                .getOrComputeIfAbsent(TestEnvironment.class, key -> TestEnvironment.start(), TestEnvironment.class);
    }
}
