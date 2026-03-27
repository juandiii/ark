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
import xyz.juandiii.ark.exceptions.ArkException;
import xyz.juandiii.ark.exceptions.ConnectionException;
import xyz.juandiii.ark.exceptions.NotFoundException;
import xyz.juandiii.ark.exceptions.ServerException;
import xyz.juandiii.ark.exceptions.TimeoutException;
import xyz.juandiii.ark.http.RawResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
        void given404Endpoint_whenSend_thenEmitsNotFoundException() {
            Mono<RawResponse> result = transport().send("GET", baseUri.resolve("/not-found"),
                    Map.of(), null, null);

            StepVerifier.create(result)
                    .expectErrorSatisfies(error -> {
                        assertInstanceOf(NotFoundException.class, error);
                        ApiException ex = (ApiException) error;
                        assertEquals(404, ex.statusCode());
                        assertEquals("Not Found", ex.responseBody());
                    })
                    .verify();
        }

        @Test
        void given500Endpoint_whenSend_thenEmitsServerException() {
            Mono<RawResponse> result = transport().send("GET", baseUri.resolve("/server-error"),
                    Map.of(), null, null);

            StepVerifier.create(result)
                    .expectErrorSatisfies(error -> {
                        assertInstanceOf(ServerException.class, error);
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
        void givenTimeout_whenSlowEndpoint_thenEmitsTimeoutException() {
            Mono<RawResponse> result = transport().send("GET", baseUri.resolve("/slow"),
                    Map.of(), null, Duration.ofMillis(100));

            StepVerifier.create(result)
                    .expectErrorSatisfies(error ->
                            assertInstanceOf(TimeoutException.class, error))
                    .verify(Duration.ofSeconds(5));
        }

        @Test
        void givenConnectionRefused_whenSend_thenEmitsConnectionException() {
            Mono<RawResponse> result = transport().send("GET",
                    URI.create("http://localhost:1/refused"), Map.of(), null, Duration.ofSeconds(2));

            StepVerifier.create(result)
                    .expectErrorSatisfies(error ->
                            assertInstanceOf(ConnectionException.class, error))
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
    class MalformedResponse {

        @Test
        void givenMalformedResponse_whenSend_thenEmitsArkException() throws Exception {
            // Raw TCP server that sends invalid HTTP
            try (ServerSocket ss = new ServerSocket(0)) {
                int port = ss.getLocalPort();
                Thread serverThread = new Thread(() -> {
                    try (Socket client = ss.accept();
                         OutputStream out = client.getOutputStream()) {
                        // Send garbage instead of valid HTTP
                        out.write("GARBAGE_NOT_HTTP\r\n\r\n".getBytes());
                        out.flush();
                    } catch (IOException ignored) {}
                });
                serverThread.setDaemon(true);
                serverThread.start();

                Mono<RawResponse> result = transport().send("GET",
                        URI.create("http://localhost:" + port + "/malformed"),
                        Map.of(), null, Duration.ofSeconds(5));

                StepVerifier.create(result)
                        .expectErrorSatisfies(error ->
                                assertInstanceOf(ArkException.class, error))
                        .verify(Duration.ofSeconds(10));
            }
        }
    }

    @Nested
    class RandomDataThenClose {

        @Test
        void givenRandomDataThenClose_whenSend_thenEmitsArkException() throws Exception {
            try (ServerSocket ss = new ServerSocket(0)) {
                int port = ss.getLocalPort();
                Thread serverThread = new Thread(() -> {
                    try (Socket client = ss.accept();
                         OutputStream out = client.getOutputStream()) {
                        // Send random bytes then close abruptly
                        out.write(new byte[]{0x00, 0x1F, (byte) 0xFF, 0x42, 0x13});
                        out.flush();
                    } catch (IOException ignored) {}
                });
                serverThread.setDaemon(true);
                serverThread.start();

                Mono<RawResponse> result = transport().send("GET",
                        URI.create("http://localhost:" + port + "/random"),
                        Map.of(), null, Duration.ofSeconds(5));

                StepVerifier.create(result)
                        .expectErrorSatisfies(error ->
                                assertInstanceOf(ArkException.class, error))
                        .verify(Duration.ofSeconds(10));
            }
        }
    }

    @Nested
    class ConnectionReset {

        @Test
        void givenConnectionReset_whenSend_thenEmitsArkException() throws Exception {
            // Server accepts connection and closes immediately without responding
            try (ServerSocket ss = new ServerSocket(0)) {
                int port = ss.getLocalPort();
                Thread serverThread = new Thread(() -> {
                    try (Socket client = ss.accept()) {
                        // Accept and close immediately — connection reset
                    } catch (IOException ignored) {}
                });
                serverThread.setDaemon(true);
                serverThread.start();

                Mono<RawResponse> result = transport().send("GET",
                        URI.create("http://localhost:" + port + "/reset"),
                        Map.of(), null, Duration.ofSeconds(5));

                StepVerifier.create(result)
                        .expectErrorSatisfies(error ->
                                assertInstanceOf(ArkException.class, error))
                        .verify(Duration.ofSeconds(10));
            }
        }
    }

    @Nested
    class PartialResponse {

        @Test
        void givenPartialResponse_whenSend_thenEmitsArkException() throws Exception {
            // Server sends valid headers with Content-Length but only partial body then closes
            try (ServerSocket ss = new ServerSocket(0)) {
                int port = ss.getLocalPort();
                Thread serverThread = new Thread(() -> {
                    try (Socket client = ss.accept();
                         OutputStream out = client.getOutputStream()) {
                        String headers = "HTTP/1.1 200 OK\r\nContent-Length: 1000\r\n\r\n";
                        out.write(headers.getBytes());
                        out.write("partial".getBytes());
                        out.flush();
                        // Close without sending the remaining 993 bytes
                    } catch (IOException ignored) {}
                });
                serverThread.setDaemon(true);
                serverThread.start();

                Mono<RawResponse> result = transport().send("GET",
                        URI.create("http://localhost:" + port + "/partial"),
                        Map.of(), null, Duration.ofSeconds(5));

                StepVerifier.create(result)
                        .expectErrorSatisfies(error ->
                                assertInstanceOf(ArkException.class, error))
                        .verify(Duration.ofSeconds(10));
            }
        }
    }

    @Nested
    class EmptyResponse {

        @Test
        void givenEmptyResponse_whenSend_thenEmitsArkException() throws Exception {
            // Server accepts connection, sends nothing, and closes
            try (ServerSocket ss = new ServerSocket(0)) {
                int port = ss.getLocalPort();
                Thread serverThread = new Thread(() -> {
                    try (Socket client = ss.accept()) {
                        // Read the request but don't respond
                        client.getInputStream().readAllBytes();
                    } catch (IOException ignored) {}
                });
                serverThread.setDaemon(true);
                serverThread.start();

                Mono<RawResponse> result = transport().send("GET",
                        URI.create("http://localhost:" + port + "/empty"),
                        Map.of(), null, Duration.ofSeconds(5));

                StepVerifier.create(result)
                        .expectErrorSatisfies(error ->
                                assertInstanceOf(ArkException.class, error))
                        .verify(Duration.ofSeconds(10));
            }
        }
    }

    @Nested
    class SlowHeaders {

        @Test
        void givenSlowHeaders_whenSendWithTimeout_thenEmitsTimeoutException() throws Exception {
            // Server accepts connection but waits before sending any response
            try (ServerSocket ss = new ServerSocket(0)) {
                int port = ss.getLocalPort();
                Thread serverThread = new Thread(() -> {
                    try (Socket client = ss.accept();
                         OutputStream out = client.getOutputStream()) {
                        // Wait longer than the client timeout before sending anything
                        Thread.sleep(5000);
                        out.write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nok".getBytes());
                        out.flush();
                    } catch (IOException | InterruptedException ignored) {}
                });
                serverThread.setDaemon(true);
                serverThread.start();

                Mono<RawResponse> result = transport().send("GET",
                        URI.create("http://localhost:" + port + "/slow-headers"),
                        Map.of(), null, Duration.ofMillis(200));

                StepVerifier.create(result)
                        .expectErrorSatisfies(error ->
                                assertInstanceOf(TimeoutException.class, error))
                        .verify(Duration.ofSeconds(10));
            }
        }
    }

    @Nested
    class HeadersOnlyNoBody {

        @Test
        void givenHeadersOnlyNoBody_whenSendWithTimeout_thenEmitsTimeoutException() throws Exception {
            // Server sends headers with Content-Length but never sends body and never closes
            try (ServerSocket ss = new ServerSocket(0)) {
                int port = ss.getLocalPort();
                Thread serverThread = new Thread(() -> {
                    try (Socket client = ss.accept();
                         OutputStream out = client.getOutputStream()) {
                        String headers = "HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\n";
                        out.write(headers.getBytes());
                        out.flush();
                        // Keep connection open, never send body
                        Thread.sleep(10000);
                    } catch (IOException | InterruptedException ignored) {}
                });
                serverThread.setDaemon(true);
                serverThread.start();

                Mono<RawResponse> result = transport().send("GET",
                        URI.create("http://localhost:" + port + "/headers-only"),
                        Map.of(), null, Duration.ofMillis(200));

                StepVerifier.create(result)
                        .expectErrorSatisfies(error ->
                                assertInstanceOf(TimeoutException.class, error))
                        .verify(Duration.ofSeconds(10));
            }
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