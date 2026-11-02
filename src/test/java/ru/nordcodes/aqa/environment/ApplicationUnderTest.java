package ru.nordcodes.aqa.environment;

import org.awaitility.Awaitility;
import ru.nordcodes.aqa.config.TestConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Тестируемое приложение, запущенное отдельным процессом из JAR.
 * JAR не изменяется: передаются только параметры запуска из документации
 * ({@code -Dsecret}, {@code -Dmock}) и порт HTTP-сервера.
 */
final class ApplicationUnderTest implements AutoCloseable {

    static final Path LOG_FILE = Path.of("target", "application.log");

    private final Process process;
    private final String baseUrl;

    private ApplicationUnderTest(Process process, String baseUrl) {
        this.process = process;
        this.baseUrl = baseUrl;
    }

    static ApplicationUnderTest start(String mockUrl) {
        Path jar = TestConfig.appJar();
        if (!Files.isRegularFile(jar)) {
            throw new IllegalStateException("Не найден тестируемый JAR: " + jar
                    + ". Положите его по этому пути или укажите -Dapp.jar=<путь>");
        }
        int port = TestConfig.appPort() == 0 ? freePort() : TestConfig.appPort();
        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        List<String> command = List.of(javaBin,
                "-Dsecret=" + TestConfig.apiKey(),
                "-Dmock=" + mockUrl,
                "-Dserver.port=" + port,
                "-jar", jar.toString());
        try {
            Files.createDirectories(LOG_FILE.getParent());
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(LOG_FILE.toFile())
                    .start();
            ApplicationUnderTest app = new ApplicationUnderTest(process, "http://localhost:" + port);
            app.awaitReady();
            return app;
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось запустить приложение", e);
        }
    }

    String baseUrl() {
        return baseUrl;
    }

    /** Приложение готово, когда HTTP-сервер отвечает на любой запрос. */
    private void awaitReady() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
        HttpRequest probe = HttpRequest.newBuilder(URI.create(baseUrl + "/endpoint"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            Awaitility.await("старт тестируемого приложения")
                    .atMost(TestConfig.appStartupTimeout())
                    .pollInterval(Duration.ofMillis(500))
                    .ignoreExceptions()
                    .failFast("процесс приложения завершился, см. " + LOG_FILE.toAbsolutePath(),
                            () -> !process.isAlive())
                    .until(() -> client.send(probe, HttpResponse.BodyHandlers.discarding()).statusCode() > 0);
        } catch (RuntimeException e) {
            close();
            throw e;
        }
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        process.destroy();
        try {
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }
}
