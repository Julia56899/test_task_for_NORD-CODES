package com.aqa.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/**
 * Поднимает один раз на весь прогон тестов: заглушку внешнего сервиса (WireMock, порт 8888)
 * и тестируемое приложение (порт 8080). Останавливает их при завершении прогона.
 */
public final class TestEnvironment {

    public static final int APP_PORT = 8080;
    public static final int MOCK_PORT = 8888;
    public static final String API_KEY = "qazWSXedc";

    private static WireMockServer mock;
    private static Process app;

    private TestEnvironment() {
    }

    public static synchronized WireMockServer mock() {
        if (mock == null) {
            start();
        }
        return mock;
    }

    private static void start() {
        mock = new WireMockServer(WireMockConfiguration.options().port(MOCK_PORT));
        mock.start();

        Path jar = Path.of(System.getProperty("app.jar", "app/internal-0.0.1-SNAPSHOT.jar")).toAbsolutePath();
        if (!jar.toFile().exists()) {
            throw new IllegalStateException("Не найден jar тестируемого приложения: " + jar
                    + ". Положите его в папку app/ или укажите путь: -Dapp.jar=<путь>");
        }
        if (isPortOpen(APP_PORT)) {
            throw new IllegalStateException("Порт " + APP_PORT + " уже занят. Остановите запущенное приложение.");
        }
        try {
            File log = new File("target/app.log");
            log.getParentFile().mkdirs();
            app = new ProcessBuilder("java", "-jar",
                    "-Dsecret=" + API_KEY,
                    "-Dmock=http://localhost:" + MOCK_PORT + "/",
                    jar.toString())
                    .redirectErrorStream(true)
                    .redirectOutput(log)
                    .start();
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось запустить приложение", e);
        }
        waitForPort(APP_PORT, Duration.ofSeconds(60));
        Runtime.getRuntime().addShutdownHook(new Thread(TestEnvironment::stop));
    }

    private static void stop() {
        if (app != null) {
            app.destroy();
        }
        if (mock != null) {
            mock.stop();
        }
    }

    private static void waitForPort(int port, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (isPortOpen(port)) {
                return;
            }
            if (!app.isAlive()) {
                throw new IllegalStateException("Приложение завершилось при запуске, см. target/app.log");
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        throw new IllegalStateException("Приложение не запустилось за " + timeout.toSeconds() + " сек, см. target/app.log");
    }

    private static boolean isPortOpen(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 200);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
