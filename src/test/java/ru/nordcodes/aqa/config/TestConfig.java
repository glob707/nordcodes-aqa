package ru.nordcodes.aqa.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;

/**
 * Единая точка конфигурации тестов.
 * Значения читаются из {@code test.properties}; любое из них можно переопределить
 * системным свойством, например {@code mvn test -Dapp.jar=/path/to/app.jar}.
 */
public final class TestConfig {

    private static final Properties PROPERTIES = load();

    private TestConfig() {
    }

    public static Path appJar() {
        return Path.of(get("app.jar")).toAbsolutePath();
    }

    public static String apiKey() {
        return get("app.secret");
    }

    public static int appPort() {
        return Integer.parseInt(get("app.port"));
    }

    public static Duration appStartupTimeout() {
        return Duration.ofSeconds(Long.parseLong(get("app.startup.timeout.seconds")));
    }

    /** Адрес уже запущенного приложения; пусто — тесты запускают JAR сами. */
    public static Optional<String> externalAppUrl() {
        return Optional.of(get("app.url")).filter(url -> !url.isBlank());
    }

    public static int mockPort() {
        return Integer.parseInt(get("mock.port"));
    }

    public static Duration externalReadTimeout() {
        return Duration.ofSeconds(Long.parseLong(get("external.read.timeout.seconds")));
    }

    private static String get(String key) {
        String value = System.getProperty(key, PROPERTIES.getProperty(key));
        if (value == null) {
            throw new IllegalStateException("В test.properties не задан параметр '" + key + "'");
        }
        return value.trim();
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = TestConfig.class.getClassLoader().getResourceAsStream("test.properties")) {
            if (in == null) {
                throw new IllegalStateException("Не найден файл test.properties");
            }
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties;
    }
}
