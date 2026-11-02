package ru.nordcodes.aqa.environment;

import com.github.tomakehurst.wiremock.WireMockServer;
import ru.nordcodes.aqa.config.TestConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Окружение одного прогона тестов: WireMock в роли внешнего сервиса
 * и тестируемое приложение, настроенное на этот WireMock.
 * Создаётся один раз на весь прогон, так как запуск приложения занимает несколько секунд.
 */
final class TestEnvironment implements AutoCloseable {

    private final WireMockServer wireMock;
    private final ApplicationUnderTest application;
    private final String appUrl;

    private TestEnvironment(WireMockServer wireMock, ApplicationUnderTest application, String appUrl) {
        this.wireMock = wireMock;
        this.application = application;
        this.appUrl = appUrl;
    }

    static TestEnvironment start() {
        WireMockServer wireMock = new WireMockServer(options().port(TestConfig.mockPort()));
        wireMock.start();
        try {
            ApplicationUnderTest application = null;
            String appUrl = TestConfig.externalAppUrl().orElse(null);
            if (appUrl == null) {
                application = ApplicationUnderTest.start(wireMock.baseUrl() + "/");
                appUrl = application.baseUrl();
            }
            TestEnvironment environment = new TestEnvironment(wireMock, application, appUrl);
            environment.writeAllureMetadata();
            return environment;
        } catch (RuntimeException e) {
            wireMock.stop();
            throw e;
        }
    }

    WireMockServer wireMock() {
        return wireMock;
    }

    String appUrl() {
        return appUrl;
    }

    /** Описание окружения и категорий ошибок для вкладок Overview и Categories в Allure. */
    private void writeAllureMetadata() {
        Path results = Path.of(System.getProperty("allure.results.directory", "target/allure-results"));
        try {
            Files.createDirectories(results);
            Files.write(results.resolve("environment.properties"), List.of(
                    "Тестируемое.приложение=" + TestConfig.appJar().getFileName(),
                    "Адрес.приложения=" + appUrl,
                    "Внешний.сервис.(WireMock)=" + wireMock.baseUrl(),
                    "Java=" + System.getProperty("java.version")));
            try (InputStream categories = getClass().getClassLoader().getResourceAsStream("allure-categories.json")) {
                if (categories != null) {
                    Files.copy(categories, results.resolve("categories.json"), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        if (application != null) {
            application.close();
        }
        wireMock.stop();
    }
}
