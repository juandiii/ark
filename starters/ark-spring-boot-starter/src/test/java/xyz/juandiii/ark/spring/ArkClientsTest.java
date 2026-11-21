package xyz.juandiii.ark.spring;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.core.exceptions.ResponseTooLargeException;
import xyz.juandiii.ark.spring.clients.ClientsConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArkClientsTest {

    static HttpServer server;

    final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ArkAutoConfiguration.class))
            .withUserConfiguration(ClientsConfig.class)
            .withPropertyValues(
                    "ark.client.src.base-url=http://localhost:" + server.getAddress().getPort(),
                    "ark.client.src.max-response-bytes=100",
                    "ark.client.src.headers.X-Test=yes");

    @BeforeAll
    static void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/ok", ex -> write(ex, "ok".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/big", ex -> {
            byte[] big = new byte[1_000];
            Arrays.fill(big, (byte) 'a');
            write(ex, big);
        });
        server.createContext("/echo-header", ex -> {
            String v = ex.getRequestHeaders().getFirst("X-Test");
            write(ex, (v == null ? "" : v).getBytes(StandardCharsets.UTF_8));
        });
        server.start();
    }

    @AfterAll
    static void stop() {
        server.stop(0);
    }

    private static void write(HttpExchange ex, byte[] body) throws IOException {
        ex.sendResponseHeaders(200, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    @Test
    void builderAppliesBaseUrlAndHeaders() {
        runner.run(ctx -> {
            String body = ctx.getBean(ArkClients.class).builder("src").build()
                    .get("/echo-header").retrieve().body(String.class);
            assertEquals("yes", body);
        });
    }

    @Test
    void builderAppliesMaxResponseBytes() {
        runner.run(ctx -> {
            var client = ctx.getBean(ArkClients.class).builder("src").build();
            assertThrows(ResponseTooLargeException.class, () -> client.get("/big").retrieve().raw());
        });
    }

    @Test
    void asyncBuilderAppliesMaxResponseBytes() {
        runner.run(ctx -> {
            var client = ctx.getBean(ArkClients.class).asyncBuilder("src").build();
            var e = assertThrows(CompletionException.class, () -> client.get("/big").retrieve().raw().join());
            assertInstanceOf(ResponseTooLargeException.class, e.getCause());
        });
    }

    @Test
    void retrieveStreamWorksWhenRetryIsOn() {
        runner.withPropertyValues("ark.client.src.retry.max-attempts=2").run(ctx -> {
            var client = ctx.getBean(ArkClients.class).builder("src").build();
            try (var response = client.get("/ok").retrieveStream()) {
                assertEquals("ok", new String(response.body().readAllBytes(), StandardCharsets.UTF_8));
            }
        });
    }

    @Test
    void unknownKeyThrows() {
        runner.run(ctx -> assertThrows(ArkException.class,
                () -> ctx.getBean(ArkClients.class).builder("missing")));
    }
}
