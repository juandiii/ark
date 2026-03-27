package xyz.juandiii.ark.transport.vertx.mutiny;

import com.sun.net.httpserver.HttpServer;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.juandiii.ark.exceptions.ApiException;
import xyz.juandiii.ark.exceptions.ArkException;
import xyz.juandiii.ark.exceptions.NotFoundException;
import xyz.juandiii.ark.exceptions.ServerException;
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

class ArkVertxMutinyTransportTest {

    private static HttpServer server;
    private static URI baseUri;
    private static Vertx vertx;

    @BeforeAll
    static void startServer() throws IOException {
        vertx = Vertx.vertx();
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/ok", exchange -> {
            exchange.getResponseHeaders().add("X-Custom", "mutiny-value");
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
        vertx.closeAndAwait();
    }

    private ArkVertxMutinyTransport transport() {
        return new ArkVertxMutinyTransport(WebClient.create(vertx));
    }

    @Nested
    class Constructor {

        @Test
        void givenNullWebClient_whenConstructing_thenThrowsNullPointerException() {
            assertThrows(NullPointerException.class, () -> new ArkVertxMutinyTransport(null));
        }

        @Test
        void givenValidWebClient_whenConstructing_thenSucceeds() {
            assertDoesNotThrow(() -> new ArkVertxMutinyTransport(WebClient.create(vertx)));
        }
    }

    @Nested
    class Send {

        @Test
        void givenSuccessEndpoint_whenSend_thenReturnsUniWith200() {
            Uni<RawResponse> result = transport().send("GET", baseUri.resolve("/ok"),
                    Map.of(), null, null);

            RawResponse response = result.await().atMost(Duration.ofSeconds(10));
            assertEquals(200, response.statusCode());
            assertEquals("{\"status\":\"ok\"}", response.body());
            assertFalse(response.isError());
        }

        @Test
        void givenSuccessEndpoint_whenSend_thenResponseContainsHeaders() {
            Uni<RawResponse> result = transport().send("GET", baseUri.resolve("/ok"),
                    Map.of(), null, null);

            RawResponse response = result.await().atMost(Duration.ofSeconds(10));
            boolean hasHeader = response.headers().keySet().stream()
                    .anyMatch(k -> k.equalsIgnoreCase("X-Custom"));
            assertTrue(hasHeader);
        }

        @Test
        void given404Endpoint_whenSend_thenUniFailsWithNotFoundException() {
            Uni<RawResponse> result = transport().send("GET", baseUri.resolve("/not-found"),
                    Map.of(), null, null);

            UniAssertSubscriber<RawResponse> subscriber = result
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.awaitFailure(Duration.ofSeconds(10));
            Throwable failure = subscriber.getFailure();
            assertInstanceOf(NotFoundException.class, failure);
            assertEquals(404, ((ApiException) failure).statusCode());
            assertEquals("Not Found", ((ApiException) failure).responseBody());
        }

        @Test
        void given500Endpoint_whenSend_thenUniFailsWithServerException() {
            Uni<RawResponse> result = transport().send("GET", baseUri.resolve("/server-error"),
                    Map.of(), null, null);

            UniAssertSubscriber<RawResponse> subscriber = result
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.awaitFailure(Duration.ofSeconds(10));
            assertInstanceOf(ServerException.class, subscriber.getFailure());
            assertEquals(500, ((ApiException) subscriber.getFailure()).statusCode());
        }

        @Test
        void givenPostMethod_whenSend_thenServerReceivesCorrectMethod() {
            RawResponse response = transport().send("POST", baseUri.resolve("/echo-method"),
                    Map.of(), "", null).await().atMost(Duration.ofSeconds(10));

            assertEquals("POST", response.body());
        }

        @Test
        void givenPutMethod_whenSend_thenServerReceivesCorrectMethod() {
            RawResponse response = transport().send("PUT", baseUri.resolve("/echo-method"),
                    Map.of(), "", null).await().atMost(Duration.ofSeconds(10));

            assertEquals("PUT", response.body());
        }

        @Test
        void givenDeleteMethod_whenSend_thenServerReceivesCorrectMethod() {
            RawResponse response = transport().send("DELETE", baseUri.resolve("/echo-method"),
                    Map.of(), null, null).await().atMost(Duration.ofSeconds(10));

            assertEquals("DELETE", response.body());
        }

        @Test
        void givenBody_whenSend_thenServerReceivesBody() {
            RawResponse response = transport().send("POST", baseUri.resolve("/echo-body"),
                    Map.of(), "{\"name\":\"Juan\"}", null).await().atMost(Duration.ofSeconds(10));

            assertEquals("{\"name\":\"Juan\"}", response.body());
        }

        @Test
        void givenNullBody_whenSend_thenServerReceivesNoBody() {
            RawResponse response = transport().send("GET", baseUri.resolve("/echo-body"),
                    Map.of(), null, null).await().atMost(Duration.ofSeconds(10));

            assertTrue(response.body() == null || response.body().isEmpty(),
                    "Expected empty or null body but got: " + response.body());
        }

        @Test
        void givenHeaders_whenSend_thenServerReceivesHeaders() {
            RawResponse response = transport().send("GET", baseUri.resolve("/echo-header"),
                    Map.of("Authorization", "Bearer mutiny-token"), null, null)
                    .await().atMost(Duration.ofSeconds(10));

            assertEquals("Bearer mutiny-token", response.body());
        }

        @Test
        void givenTimeout_whenSlowEndpoint_thenUniFailsWithArkException() {
            Uni<RawResponse> result = transport().send("GET", baseUri.resolve("/slow"),
                    Map.of(), null, Duration.ofMillis(100));

            UniAssertSubscriber<RawResponse> subscriber = result
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.awaitFailure(Duration.ofSeconds(10));
            assertInstanceOf(ArkException.class, subscriber.getFailure());
        }

        @Test
        void givenConnectionRefused_whenSend_thenUniFailsWithArkException() {
            Uni<RawResponse> result = transport().send("GET",
                    URI.create("http://localhost:1/refused"), Map.of(), null, Duration.ofSeconds(2));

            UniAssertSubscriber<RawResponse> subscriber = result
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.awaitFailure(Duration.ofSeconds(10));
            assertInstanceOf(ArkException.class, subscriber.getFailure());
        }

        @Test
        void givenConnectionReset_whenSend_thenUniFailsWithArkException() throws Exception {
            try (ServerSocket ss = new ServerSocket(0)) {
                int port = ss.getLocalPort();
                Thread t = new Thread(() -> {
                    try (Socket c = ss.accept()) {
                    } catch (IOException ignored) {}
                });
                t.setDaemon(true);
                t.start();

                Uni<RawResponse> result = transport().send("GET",
                        URI.create("http://localhost:" + port + "/reset"), Map.of(), null, Duration.ofSeconds(5));

                UniAssertSubscriber<RawResponse> subscriber = result
                        .subscribe().withSubscriber(UniAssertSubscriber.create());
                subscriber.awaitFailure(Duration.ofSeconds(10));
                assertInstanceOf(ArkException.class, subscriber.getFailure());
            }
        }

        @Test
        void givenMalformedResponse_whenSend_thenUniFailsWithArkException() throws Exception {
            try (ServerSocket ss = new ServerSocket(0)) {
                int port = ss.getLocalPort();
                Thread t = new Thread(() -> {
                    try (Socket c = ss.accept(); OutputStream o = c.getOutputStream()) {
                        o.write("GARBAGE_NOT_HTTP\r\n\r\n".getBytes());
                        o.flush();
                    } catch (IOException ignored) {}
                });
                t.setDaemon(true);
                t.start();

                Uni<RawResponse> result = transport().send("GET",
                        URI.create("http://localhost:" + port + "/malformed"), Map.of(), null, Duration.ofSeconds(5));

                UniAssertSubscriber<RawResponse> subscriber = result
                        .subscribe().withSubscriber(UniAssertSubscriber.create());
                subscriber.awaitFailure(Duration.ofSeconds(10));
                assertInstanceOf(ArkException.class, subscriber.getFailure());
            }
        }

        @Test
        void givenSlowHeaders_whenSendWithTimeout_thenUniFailsWithArkException() throws Exception {
            try (ServerSocket ss = new ServerSocket(0)) {
                int port = ss.getLocalPort();
                Thread t = new Thread(() -> {
                    try (Socket c = ss.accept(); OutputStream o = c.getOutputStream()) {
                        Thread.sleep(5000);
                        o.write("HTTP/1.1 200 OK\r\n\r\nok".getBytes());
                    } catch (IOException | InterruptedException ignored) {}
                });
                t.setDaemon(true);
                t.start();

                Uni<RawResponse> result = transport().send("GET",
                        URI.create("http://localhost:" + port + "/slow-headers"), Map.of(), null, Duration.ofMillis(200));

                UniAssertSubscriber<RawResponse> subscriber = result
                        .subscribe().withSubscriber(UniAssertSubscriber.create());
                subscriber.awaitFailure(Duration.ofSeconds(10));
                assertInstanceOf(ArkException.class, subscriber.getFailure());
            }
        }
    }

    @Nested
    class ImplementsInterface {

        @Test
        void givenTransport_whenChecked_thenImplementsMutinyHttpTransport() {
            assertInstanceOf(xyz.juandiii.ark.mutiny.http.MutinyHttpTransport.class, transport());
        }
    }
}