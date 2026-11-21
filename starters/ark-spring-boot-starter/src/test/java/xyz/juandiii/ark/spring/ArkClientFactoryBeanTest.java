package xyz.juandiii.ark.spring;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import xyz.juandiii.ark.core.exceptions.ApiException;
import xyz.juandiii.ark.spring.clients.ClientsConfig;
import xyz.juandiii.ark.spring.clients.ReactiveApi;
import xyz.juandiii.ark.spring.clients.SyncApi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArkClientFactoryBeanTest {

    static HttpServer server;
    static final AtomicInteger hits = new AtomicInteger();

    final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ArkAutoConfiguration.class))
            .withUserConfiguration(ClientsConfig.class)
            .withPropertyValues("ark.client.sync-api.base-url=http://localhost:" + server.getAddress().getPort());

    @BeforeAll
    static void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/status", ex -> {
            hits.incrementAndGet();
            ex.sendResponseHeaders(503, -1);
            ex.close();
        });
        server.start();
    }

    @AfterAll
    static void stop() {
        server.stop(0);
    }

    @BeforeEach
    void reset() {
        hits.set(0);
    }

    @Test
    void registersOnlyNonReactiveInterfaces() {
        runner.run(ctx -> {
            assertEquals(1, ctx.getBeanNamesForType(SyncApi.class).length);
            assertEquals(0, ctx.getBeanNamesForType(ReactiveApi.class).length,
                    "Mono/Flux interfaces belong to the webflux starter");
        });
    }

    @Test
    void retryStaysOffWhenMaxAttemptsIsNotSet() {
        runner.withPropertyValues("ark.client.sync-api.retry.delay=1").run(ctx -> {
            assertThrows(ApiException.class, () -> ctx.getBean(SyncApi.class).status());
            assertEquals(1, hits.get());
        });
    }

    @Test
    void retriesWhenMaxAttemptsIsAboveOne() {
        runner.withPropertyValues(
                "ark.client.sync-api.retry.max-attempts=3",
                "ark.client.sync-api.retry.delay=1").run(ctx -> {
            assertThrows(ApiException.class, () -> ctx.getBean(SyncApi.class).status());
            assertEquals(3, hits.get());
        });
    }
}
