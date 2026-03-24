package xyz.juandiii.ark.transport.vertx;

import com.sun.net.httpserver.HttpServer;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.juandiii.ark.exceptions.ApiException;
import xyz.juandiii.ark.http.RawResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ArkVertxFutureTransportTest {

    private static HttpServer server;
    private static URI baseUri;
    private static Vertx vertx;

    @BeforeAll
    static void startServer() throws IOException {
        vertx = Vertx.vertx();
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/ok", exchange -> {
            exchange.getResponseHeaders().add("X-Custom", "vertx-value");
            byte[] body = "{\"status\":\"ok\"}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
        });

        server.createContext("/not-found", exchange -> {
            byte[] body = "Not Found".getBytes();
            exchange.sendResponseHeaders(404, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
        });

        server.createContext("/server-error", exchange -> {
            byte[] body = "Internal Server Error".getBytes();
            exchange.sendResponseHeaders(500, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
        });

        server.createContext("/echo-method", exchange -> {
            byte[] body = exchange.getRequestMethod().getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
        });

        server.createContext("/echo-body", exchange -> {
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, requestBody.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(requestBody); }
        });

        server.createContext("/echo-header", exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            byte[] body = (auth != null ? auth : "none").getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
        });

        server.createContext("/slow", exchange -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            byte[] body = "slow".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
        });

        server.start();
        baseUri = URI.create("http://localhost:" + server.getAddress().getPort());
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
        vertx.close();
    }

    private ArkVertxFutureTransport transport() {
        return new ArkVertxFutureTransport(WebClient.create(vertx));
    }

    private <T> T await(Future<T> future) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        future.onSuccess(r -> { result.set(r); latch.countDown(); })
              .onFailure(e -> { error.set(e); latch.countDown(); });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Future did not complete in time");

        if (error.get() != null) throw new RuntimeException(error.get());
        return result.get();
    }

    private Throwable awaitFailure(Future<?> future) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        future.onSuccess(r -> latch.countDown())
              .onFailure(e -> { error.set(e); latch.countDown(); });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Future did not complete in time");
        assertNotNull(error.get(), "Expected failure but Future succeeded");
        return error.get();
    }

    @Nested
    class Constructor {

        @Test
        void givenNullWebClient_whenConstructing_thenThrowsNullPointerException() {
            assertThrows(NullPointerException.class, () -> new ArkVertxFutureTransport(null));
        }

        @Test
        void givenValidWebClient_whenConstructing_thenSucceeds() {
            assertDoesNotThrow(() -> new ArkVertxFutureTransport(WebClient.create(vertx)));
        }

        @Test
        void givenWebClientWithOptions_whenConstructing_thenSucceeds() {
            WebClient client = WebClient.create(vertx, new WebClientOptions()
                    .setConnectTimeout(5000)
                    .setMaxPoolSize(50));
            assertDoesNotThrow(() -> new ArkVertxFutureTransport(client));
        }
    }

    @Nested
    class Send {

        @Test
        void givenSuccessEndpoint_whenSend_thenReturnsFutureWith200() throws Exception {
            RawResponse response = await(transport().send("GET", baseUri.resolve("/ok"),
                    Map.of(), null, null));

            assertEquals(200, response.statusCode());
            assertEquals("{\"status\":\"ok\"}", response.body());
            assertFalse(response.isError());
        }

        @Test
        void givenSuccessEndpoint_whenSend_thenResponseContainsHeaders() throws Exception {
            RawResponse response = await(transport().send("GET", baseUri.resolve("/ok"),
                    Map.of(), null, null));

            boolean hasHeader = response.headers().keySet().stream()
                    .anyMatch(k -> k.equalsIgnoreCase("X-Custom"));
            assertTrue(hasHeader);
        }

        @Test
        void given404Endpoint_whenSend_thenFutureFailsWithApiException() throws Exception {
            Throwable error = awaitFailure(transport().send("GET", baseUri.resolve("/not-found"),
                    Map.of(), null, null));

            assertInstanceOf(ApiException.class, error);
            ApiException ex = (ApiException) error;
            assertEquals(404, ex.statusCode());
            assertEquals("Not Found", ex.responseBody());
            assertTrue(ex.isNotFound());
        }

        @Test
        void given500Endpoint_whenSend_thenFutureFailsWithApiException() throws Exception {
            Throwable error = awaitFailure(transport().send("GET", baseUri.resolve("/server-error"),
                    Map.of(), null, null));

            assertInstanceOf(ApiException.class, error);
            assertEquals(500, ((ApiException) error).statusCode());
        }

        @Test
        void givenPostMethod_whenSend_thenServerReceivesCorrectMethod() throws Exception {
            RawResponse response = await(transport().send("POST", baseUri.resolve("/echo-method"),
                    Map.of(), "", null));

            assertEquals("POST", response.body());
        }

        @Test
        void givenPutMethod_whenSend_thenServerReceivesCorrectMethod() throws Exception {
            RawResponse response = await(transport().send("PUT", baseUri.resolve("/echo-method"),
                    Map.of(), "", null));

            assertEquals("PUT", response.body());
        }

        @Test
        void givenDeleteMethod_whenSend_thenServerReceivesCorrectMethod() throws Exception {
            RawResponse response = await(transport().send("DELETE", baseUri.resolve("/echo-method"),
                    Map.of(), null, null));

            assertEquals("DELETE", response.body());
        }

        @Test
        void givenBody_whenSend_thenServerReceivesBody() throws Exception {
            RawResponse response = await(transport().send("POST", baseUri.resolve("/echo-body"),
                    Map.of(), "{\"name\":\"Juan\"}", null));

            assertEquals("{\"name\":\"Juan\"}", response.body());
        }

        @Test
        void givenNullBody_whenSend_thenServerReceivesNoBody() throws Exception {
            RawResponse response = await(transport().send("GET", baseUri.resolve("/echo-body"),
                    Map.of(), null, null));

            assertTrue(response.body() == null || response.body().isEmpty(),
                    "Expected empty or null body but got: " + response.body());
        }

        @Test
        void givenHeaders_whenSend_thenServerReceivesHeaders() throws Exception {
            RawResponse response = await(transport().send("GET", baseUri.resolve("/echo-header"),
                    Map.of("Authorization", "Bearer vertx-token"), null, null));

            assertEquals("Bearer vertx-token", response.body());
        }

        @Test
        void givenTimeout_whenSlowEndpoint_thenFutureFails() throws Exception {
            Throwable error = awaitFailure(transport().send("GET", baseUri.resolve("/slow"),
                    Map.of(), null, Duration.ofMillis(100)));

            assertNotNull(error);
        }
    }

    @Nested
    class ImplementsInterface {

        @Test
        void givenTransport_whenChecked_thenImplementsVertxHttpTransport() {
            assertInstanceOf(xyz.juandiii.ark.vertx.http.VertxHttpTransport.class, transport());
        }
    }
}
