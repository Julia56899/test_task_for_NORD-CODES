package com.aqa.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.atomic.AtomicLong;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/** Общая основа тестов: чистый «внешний сервис» перед каждым тестом и генератор уникальных токенов. */
public abstract class BaseTest {

    private static final AtomicLong COUNTER = new AtomicLong(System.nanoTime());

    protected WireMockServer externalService;

    @BeforeEach
    void prepareExternalService() {
        externalService = TestEnvironment.mock();
        externalService.resetAll();
        stubExternal("/auth", 200);
        stubExternal("/doAction", 200);
    }

    protected void stubExternal(String path, int status) {
        externalService.stubFor(post(urlEqualTo(path)).willReturn(aResponse().withStatus(status)));
    }

    /** Новый корректный токен (32 символа 0-9A-F), чтобы тесты не влияли друг на друга. */
    protected static String newToken() {
        return String.format("%032X", COUNTER.incrementAndGet());
    }
}
