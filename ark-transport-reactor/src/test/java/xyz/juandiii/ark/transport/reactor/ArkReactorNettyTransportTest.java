package xyz.juandiii.ark.transport.reactor;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;
import xyz.juandiii.ark.exceptions.ApiException;
import xyz.juandiii.ark.http.RawResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArkReactorNettyTransportTest {

    private static HttpServer server;
    private static URI baseUri;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/ok", exchange -> {
            exchange.getResponseHeaders().add("X-Custom", "reactor-value");
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

        server.createContext("/no-body", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
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
    }

    private ArkReactorNettyTransport transport() {
        return new ArkReactorNettyTransport(HttpClient.create());
    }

    @Nested
    class Constructor {

        @Test
        void givenNullHttpClient_whenConstructing_thenThrowsNullPointerException() {
            assertThrows(NullPointerException.class, () -> new ArkReactorNettyTransport(null));
        }

        @Test
        void givenValidHttpClient_whenConstructing_thenSucceeds() {
            assertDoesNotThrow(() -> new ArkReactorNettyTransport(HttpClient.create()));
        }
    }

    @Nested
    class Send {

        @Test
        void givenSuccessEndpoint_whenSend_thenReturnsMono200() {
            Mono<RawResponse> result = transport().send("GET", baseUri.resolve("/ok"),
                    Map.of(), null, null);

            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertEquals(200, response.statusCode());
                        assertEquals("{\"status\":\"ok\"}", response.body());
                        assertFalse(response.isError());
                    })
                    .verifyComplete();
        }

        @Test
        void givenSuccessEndpoint_whenSend_thenResponseContainsHeaders() {
            Mono<RawResponse> result = transport().send("GET", baseUri.resolve("/ok"),
                    Map.of(), null, null);

            StepVerifier.create(result)
                    .assertNext(response -> {
                        boolean hasHeader = response.headers().keySet().stream()
                                .anyMatch(k -> k.equalsIgnoreCase("X-Custom"));
                        assertTrue(hasHeader, "Expected X-Custom header in: " + response.headers().keySet());
                    })
                    .verifyComplete();
        }

        @Test
        void given404Endpoint_whenSend_thenEmitsApiException() {
            Mono<RawResponse> result = transport().send("GET", baseUri.resolve("/not-found"),
                    Map.of(), null, null);

            StepVerifier.create(result)
                    .expectErrorSatisfies(error -> {
                        assertInstanceOf(ApiException.class, error);
                        ApiException ex = (ApiException) error;
                        assertEquals(404, ex.statusCode());
                        assertEquals("Not Found", ex.responseBody());
                    })
                    .verify();
        }

        @Test
        void given500Endpoint_whenSend_thenEmitsApiException() {
            Mono<RawResponse> result = transport().send("GET", baseUri.resolve("/server-error"),
                    Map.of(), null, null);

            StepVerifier.create(result)
                    .expectErrorSatisfies(error -> {
                        assertInstanceOf(ApiException.class, error);
                        assertEquals(500, ((ApiException) error).statusCode());
                    })
                    .verify();
        }

        @Test
        void givenPostMethod_whenSend_thenServerReceivesCorrectMethod() {
            Mono<RawResponse> result = transport().send("POST", baseUri.resolve("/echo-method"),
                    Map.of(), "", null);

            StepVerifier.create(result)
                    .assertNext(response -> assertEquals("POST", response.body()))
                    .verifyComplete();
        }

        @Test
        void givenPutMethod_whenSend_thenServerReceivesCorrectMethod() {
            Mono<RawResponse> result = transport().send("PUT", baseUri.resolve("/echo-method"),
                    Map.of(), "", null);

            StepVerifier.create(result)
                    .assertNext(response -> assertEquals("PUT", response.body()))
                    .verifyComplete();
        }

        @Test
        void givenDeleteMethod_whenSend_thenServerReceivesCorrectMethod() {
            Mono<RawResponse> result = transport().send("DELETE", baseUri.resolve("/echo-method"),
                    Map.of(), null, null);

            StepVerifier.create(result)
                    .assertNext(response -> assertEquals("DELETE", response.body()))
                    .verifyComplete();
        }

        @Test
        void givenBody_whenSend_thenServerReceivesBody() {
            Mono<RawResponse> result = transport().send("POST", baseUri.resolve("/echo-body"),
                    Map.of(), "{\"name\":\"Juan\"}", null);

            StepVerifier.create(result)
                    .assertNext(response -> assertEquals("{\"name\":\"Juan\"}", response.body()))
                    .verifyComplete();
        }

        @Test
        void givenNullBody_whenSend_thenServerReceivesEmptyBody() {
            Mono<RawResponse> result = transport().send("GET", baseUri.resolve("/echo-body"),
                    Map.of(), null, null);

            StepVerifier.create(result)
                    .assertNext(response -> assertEquals("", response.body()))
                    .verifyComplete();
        }

        @Test
        void givenHeaders_whenSend_thenServerReceivesHeaders() {
            Mono<RawResponse> result = transport().send("GET", baseUri.resolve("/echo-header"),
                    Map.of("Authorization", "Bearer reactor-token"), null, null);

            StepVerifier.create(result)
                    .assertNext(response -> assertEquals("Bearer reactor-token", response.body()))
                    .verifyComplete();
        }

        @Test
        void givenTimeout_whenSlowEndpoint_thenEmitsTimeoutError() {
            Mono<RawResponse> result = transport().send("GET", baseUri.resolve("/slow"),
                    Map.of(), null, Duration.ofMillis(100));

            StepVerifier.create(result)
                    .expectError()
                    .verify(Duration.ofSeconds(5));
        }

        @Test
        void given204NoContent_whenSend_thenReturnsEmptyBody() {
            Mono<RawResponse> result = transport().send("GET", baseUri.resolve("/no-body"),
                    Map.of(), null, null);

            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertEquals(204, response.statusCode());
                        assertEquals("", response.body());
                    })
                    .verifyComplete();
        }
    }

    @Nested
    class ImplementsInterface {

        @Test
        void givenTransport_whenChecked_thenImplementsReactorHttpTransport() {
            assertInstanceOf(xyz.juandiii.ark.reactor.http.ReactorHttpTransport.class, transport());
        }
    }
}