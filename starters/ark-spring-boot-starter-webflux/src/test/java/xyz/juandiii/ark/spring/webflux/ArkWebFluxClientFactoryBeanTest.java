package xyz.juandiii.ark.spring.webflux;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.core.publisher.Mono;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.reactor.http.ReactorHttpTransport;
import xyz.juandiii.ark.spring.webflux.clients.ClientsConfig;
import xyz.juandiii.ark.spring.webflux.clients.ReactiveApi;
import xyz.juandiii.ark.spring.webflux.clients.SyncApi;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArkWebFluxClientFactoryBeanTest {

    static HttpServer server;
    static final AtomicInteger flakyHits = new AtomicInteger();

    final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ArkWebFluxAutoConfiguration.class))
            .withUserConfiguration(ClientsConfig.class)
            .withPropertyValues("ark.client.reactive-api.base-url=http://localhost:" + server.getAddress().getPort());

    @BeforeAll
    static void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/redirect", ex -> {
            ex.getResponseHeaders().add("Location", "/ok");
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });
        server.createContext("/ok", ex -> {
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        });
        server.createContext("/flaky", ex -> {
            flakyHits.incrementAndGet();
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
        flakyHits.set(0);
    }

    @Test
    void registersOnlyReactiveInterfaces() {
        runner.run(ctx -> {
            assertEquals(1, ctx.getBeanNamesForType(ReactiveApi.class).length);
            assertEquals(0, ctx.getBeanNamesForType(SyncApi.class).length,
                    "non-reactive interfaces belong to the sync starter");
        });
    }

    @Test
    void appliesDefaultReadTimeoutOnSharedTransport() {
        AtomicReference<Duration> lastTimeout = new AtomicReference<>();
        ReactorHttpTransport recording = (method, uri, headers, body, timeout) -> {
            lastTimeout.set(timeout);
            return Mono.just(new RawResponse(200, Map.of("Content-Type", List.of("text/plain")), "ok"));
        };
        runner.withBean("reactorHttpTransport", ReactorHttpTransport.class, () -> recording).run(ctx -> {
            ctx.getBean(ReactiveApi.class).status().block();
            assertEquals(Duration.ofSeconds(30), lastTimeout.get());
        });
    }

    @Test
    void followsRedirectsByDefault() {
        runner.run(ctx -> assertEquals(200, ctx.getBean(ReactiveApi.class).redirect().block().statusCode()));
    }

    @Test
    void returnsRedirectWhenFollowRedirectsIsFalse() {
        // Also covers the default HTTP_2 on a customized client talking to a plain-http server
        runner.withPropertyValues("ark.client.reactive-api.follow-redirects=false").run(ctx ->
                assertEquals(302, ctx.getBean(ReactiveApi.class).redirect().block().statusCode()));
    }

    @Test
    void retryStaysOffWhenMaxAttemptsIsNotSet() {
        runner.withPropertyValues("ark.client.reactive-api.retry.delay=1").run(ctx -> {
            ctx.getBean(ReactiveApi.class).flaky().onErrorResume(e -> Mono.empty()).block();
            assertEquals(1, flakyHits.get());
        });
    }

    @Test
    void retriesWhenMaxAttemptsIsAboveOne() {
        runner.withPropertyValues(
                "ark.client.reactive-api.retry.max-attempts=3",
                "ark.client.reactive-api.retry.delay=1").run(ctx -> {
            ctx.getBean(ReactiveApi.class).flaky().onErrorResume(e -> Mono.empty()).block();
            assertEquals(3, flakyHits.get());
        });
    }
}
