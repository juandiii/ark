package xyz.juandiii.ark.transport.jdk;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.juandiii.ark.async.http.AsyncHttpTransport;
import xyz.juandiii.ark.core.exceptions.*;
import xyz.juandiii.ark.core.http.HttpTransport;
import xyz.juandiii.ark.core.http.RawResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArkJdkHttpTransportTest {

    private static HttpServer server;
    private static URI baseUri;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/ok", exchange -> {
            exchange.getResponseHeaders().add("X-Custom", "test-value");
            byte[] body = "{\"status\":\"ok\"}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        server.createContext("/created", exchange -> {
            byte[] body = "{\"id\":1}".getBytes();
            exchange.sendResponseHeaders(201, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        server.createContext("/not-found", exchange -> {
            byte[] body = "Resource not found".getBytes();
            exchange.sendResponseHeaders(404, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        server.createContext("/server-error", exchange -> {
            byte[] body = "Internal Server Error".getBytes();
            exchange.sendResponseHeaders(500, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        server.createContext("/echo-method", exchange -> {
            byte[] body = exchange.getRequestMethod().getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        server.createContext("/echo-body", exchange -> {
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, requestBody.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(requestBody);
            }
        });

        server.createContext("/echo-header", exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            byte[] body = (auth != null ? auth : "none").getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        server.createContext("/slow", exchange -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            byte[] body = "slow".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        server.createContext("/no-body", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });

        server.start();
        baseUri = URI.create("http://localhost:" + server.getAddress().getPort());
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    private ArkJdkHttpTransport transport() {
        return new ArkJdkHttpTransport(HttpClient.newBuilder().build());
    }

    @Nested
    class Constructor {

        @Test
        void givenNullHttpClient_whenConstructing_thenThrowsNullPointerException() {
            assertThrows(NullPointerException.class, () -> new ArkJdkHttpTransport(null));
        }

        @Test
        void givenNullExecutor_whenConstructing_thenThrowsNullPointerException() {
            assertThrows(NullPointerException.class,
                    () -> new ArkJdkHttpTransport(HttpClient.newBuilder().build(), null));
        }

        @Test
        void givenValidHttpClient_whenConstructing_thenSucceeds() {
            assertDoesNotThrow(() -> new ArkJdkHttpTransport(HttpClient.newBuilder().build()));
        }

        @Test
        void givenCustomExecutor_whenConstructing_thenSucceeds() {
            assertDoesNotThrow(() -> new ArkJdkHttpTransport(
                    HttpClient.newBuilder().build(),
                    Executors.newSingleThreadExecutor()));
        }
    }

    @Nested
    class SyncSend {

        @Test
        void givenSuccessEndpoint_whenSend_thenReturnsRawResponseWith200() {
            RawResponse response = transport().send("GET", baseUri.resolve("/ok"),
                    Map.of(), null, null);

            assertEquals(200, response.statusCode());
            assertEquals("{\"status\":\"ok\"}", response.body());
            assertFalse(response.isError());
        }

        @Test
        void givenCreatedEndpoint_whenSend_thenReturns201() {
            RawResponse response = transport().send("POST", baseUri.resolve("/created"),
                    Map.of(), "{\"name\":\"test\"}", null);

            assertEquals(201, response.statusCode());
            assertTrue(response.body().contains("\"id\""));
        }

        @Test
        void givenSuccessEndpoint_whenSend_thenResponseContainsHeaders() {
            RawResponse response = transport().send("GET", baseUri.resolve("/ok"),
                    Map.of(), null, null);

            assertTrue(response.headers().containsKey("X-custom"));
        }

        @Test
        void given404Endpoint_whenSend_thenThrowsApiException() {
            ApiException ex = assertThrows(ApiException.class, () ->
                    transport().send("GET", baseUri.resolve("/not-found"), Map.of(), null, null));

            assertEquals(404, ex.statusCode());
            assertEquals("Resource not found", ex.responseBody());
            assertInstanceOf(NotFoundException.class, ex);
        }

        @Test
        void given500Endpoint_whenSend_thenThrowsServerException() {
            ServerException ex = assertThrows(ServerException.class, () ->
                    transport().send("GET", baseUri.resolve("/server-error"), Map.of(), null, null));

            assertEquals(500, ex.statusCode());
            assertEquals("Internal Server Error", ex.responseBody());
        }

        @Test
        void givenPostMethod_whenSend_thenServerReceivesCorrectMethod() {
            RawResponse response = transport().send("POST", baseUri.resolve("/echo-method"),
                    Map.of(), "", null);

            assertEquals("POST", response.body());
        }

        @Test
        void givenPutMethod_whenSend_thenServerReceivesCorrectMethod() {
            RawResponse response = transport().send("PUT", baseUri.resolve("/echo-method"),
                    Map.of(), "", null);

            assertEquals("PUT", response.body());
        }

        @Test
        void givenDeleteMethod_whenSend_thenServerReceivesCorrectMethod() {
            RawResponse response = transport().send("DELETE", baseUri.resolve("/echo-method"),
                    Map.of(), null, null);

            assertEquals("DELETE", response.body());
        }

        @Test
        void givenBody_whenSend_thenServerReceivesBody() {
            RawResponse response = transport().send("POST", baseUri.resolve("/echo-body"),
                    Map.of(), "{\"name\":\"Juan\"}", null);

            assertEquals("{\"name\":\"Juan\"}", response.body());
        }

        @Test
        void givenNullBody_whenSend_thenServerReceivesEmptyBody() {
            RawResponse response = transport().send("GET", baseUri.resolve("/echo-body"),
                    Map.of(), null, null);

            assertEquals("", response.body());
        }

        @Test
        void givenHeaders_whenSend_thenServerReceivesHeaders() {
            RawResponse response = transport().send("GET", baseUri.resolve("/echo-header"),
                    Map.of("Authorization", "Bearer token123"), null, null);

            assertEquals("Bearer token123", response.body());
        }

        @Test
        void givenTimeout_whenSlowEndpoint_thenThrowsTimeoutException() {
            assertThrows(TimeoutException.class, () ->
                    transport().send("GET", baseUri.resolve("/slow"),
                            Map.of(), null, Duration.ofMillis(100)));
        }

        @Test
        void givenConnectionRefused_whenSend_thenThrowsConnectionException() {
            assertThrows(ConnectionException.class, () ->
                    transport().send("GET", URI.create("http://localhost:1/refused"),
                            Map.of(), null, Duration.ofSeconds(2)));
        }

        @Test
        void givenNoTimeout_whenFastEndpoint_thenSucceeds() {
            RawResponse response = transport().send("GET", baseUri.resolve("/ok"),
                    Map.of(), null, null);

            assertNotNull(response);
        }

        @Test
        void given204NoContent_whenSend_thenReturnsEmptyBody() {
            RawResponse response = transport().send("GET", baseUri.resolve("/no-body"),
                    Map.of(), null, null);

            assertEquals(204, response.statusCode());
        }

        @Test
        void givenMalformedResponse_whenSend_thenThrowsArkException() throws Exception {
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

                assertThrows(ArkException.class, () ->
                        transport().send("GET", URI.create("http://localhost:" + port + "/malformed"),
                                Map.of(), null, Duration.ofSeconds(5)));
            }
        }

        @Test
        void givenConnectionReset_whenSend_thenThrowsArkException() throws Exception {
            try (ServerSocket ss = new ServerSocket(0)) {
                int port = ss.getLocalPort();
                Thread t = new Thread(() -> {
                    try (Socket c = ss.accept()) {
                    } catch (IOException ignored) {}
                });
                t.setDaemon(true);
                t.start();

                assertThrows(ArkException.class, () ->
                        transport().send("GET", URI.create("http://localhost:" + port + "/reset"),
                                Map.of(), null, Duration.ofSeconds(5)));
            }
        }

        @Test
        void givenPartialResponse_whenSend_thenThrowsArkException() throws Exception {
            try (ServerSocket ss = new ServerSocket(0)) {
                int port = ss.getLocalPort();
                Thread t = new Thread(() -> {
                    try (Socket c = ss.accept(); OutputStream o = c.getOutputStream()) {
                        o.write("HTTP/1.1 200 OK\r\nContent-Length: 1000\r\n\r\n".getBytes());
                        o.write("partial".getBytes());
                        o.flush();
                    } catch (IOException ignored) {}
                });
                t.setDaemon(true);
                t.start();

                assertThrows(ArkException.class, () ->
                        transport().send("GET", URI.create("http://localhost:" + port + "/partial"),
                                Map.of(), null, Duration.ofSeconds(5)));
            }
        }

        @Test
        void givenSlowHeaders_whenSendWithTimeout_thenThrowsTimeoutException() throws Exception {
            try (ServerSocket ss = new ServerSocket(0)) {
                int port = ss.getLocalPort();
                Thread t = new Thread(() -> {
                    try (Socket c = ss.accept(); OutputStream o = c.getOutputStream()) {
                        Thread.sleep(5000);
                        o.write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nok".getBytes());
                    } catch (IOException | InterruptedException ignored) {}
                });
                t.setDaemon(true);
                t.start();

                assertThrows(TimeoutException.class, () ->
                        transport().send("GET", URI.create("http://localhost:" + port + "/slow-headers"),
                                Map.of(), null, Duration.ofMillis(200)));
            }
        }

        @Test
        void givenHeadersOnlyNoBody_whenSendWithTimeout_thenThrowsArkException() throws Exception {
            try (ServerSocket ss = new ServerSocket(0)) {
                int port = ss.getLocalPort();
                Thread t = new Thread(() -> {
                    try (Socket c = ss.accept(); OutputStream o = c.getOutputStream()) {
                        o.write("HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\n".getBytes());
                        o.flush();
                        Thread.sleep(10000);
                    } catch (IOException | InterruptedException ignored) {}
                });
                t.setDaemon(true);
                t.start();

                assertThrows(ArkException.class, () ->
                        transport().send("GET", URI.create("http://localhost:" + port + "/headers-only"),
                                Map.of(), null, Duration.ofMillis(500)));
            }
        }
    }

    @Nested
    class AsyncSendAsync {

        @Test
        void givenSuccessEndpoint_whenSendAsync_thenReturnsRawResponseWith200() throws Exception {
            CompletableFuture<RawResponse> future = transport().sendAsync("GET",
                    baseUri.resolve("/ok"), Map.of(), null, null);

            RawResponse response = future.get();
            assertEquals(200, response.statusCode());
            assertEquals("{\"status\":\"ok\"}", response.body());
        }

        @Test
        void given404Endpoint_whenSendAsync_thenCompletesExceptionallyWithApiException() {
            CompletableFuture<RawResponse> future = transport().sendAsync("GET",
                    baseUri.resolve("/not-found"), Map.of(), null, null);

            ExecutionException ex = assertThrows(ExecutionException.class, future::get);
            ApiException apiEx = findCause(ex, ApiException.class);
            assertNotNull(apiEx);
            assertEquals(404, apiEx.statusCode());
        }

        @Test
        void given500Endpoint_whenSendAsync_thenCompletesExceptionallyWithApiException() {
            CompletableFuture<RawResponse> future = transport().sendAsync("GET",
                    baseUri.resolve("/server-error"), Map.of(), null, null);

            ExecutionException ex = assertThrows(ExecutionException.class, future::get);
            ApiException apiEx = findCause(ex, ApiException.class);
            assertNotNull(apiEx);
            assertEquals(500, apiEx.statusCode());
        }

        @SuppressWarnings("unchecked")
        private <T extends Throwable> T findCause(Throwable t, Class<T> type) {
            Throwable current = t;
            while (current != null) {
                if (type.isInstance(current)) return (T) current;
                current = current.getCause();
            }
            return null;
        }

        @Test
        void givenBody_whenSendAsync_thenServerReceivesBody() throws Exception {
            CompletableFuture<RawResponse> future = transport().sendAsync("POST",
                    baseUri.resolve("/echo-body"), Map.of(), "{\"async\":true}", null);

            assertEquals("{\"async\":true}", future.get().body());
        }

        @Test
        void givenHeaders_whenSendAsync_thenServerReceivesHeaders() throws Exception {
            CompletableFuture<RawResponse> future = transport().sendAsync("GET",
                    baseUri.resolve("/echo-header"),
                    Map.of("Authorization", "Bearer async-token"), null, null);

            assertEquals("Bearer async-token", future.get().body());
        }

        @Test
        void givenCustomExecutor_whenSendAsync_thenUsesExecutor() throws Exception {
            var executorUsed = new boolean[]{false};
            ArkJdkHttpTransport transport = new ArkJdkHttpTransport(
                    HttpClient.newBuilder().build(),
                    runnable -> { executorUsed[0] = true; runnable.run(); });

            transport.sendAsync("GET", baseUri.resolve("/ok"), Map.of(), null, null).get();

            assertTrue(executorUsed[0]);
        }
    }

    @Nested
    class ExceptionHandling {

        @Test
        void givenInterruptedSend_whenSend_thenThrowsArkExceptionAndRestoresInterruptFlag()
                throws IOException, InterruptedException {
            HttpClient mockClient = mock(HttpClient.class);
            when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new InterruptedException("interrupted"));

            ArkJdkHttpTransport transport = new ArkJdkHttpTransport(mockClient);

            RequestInterruptedException ex = assertThrows(RequestInterruptedException.class, () ->
                    transport.send("GET", URI.create("http://localhost/test"), Map.of(), null, null));

            assertInstanceOf(InterruptedException.class, ex.getCause());
            assertTrue(Thread.currentThread().isInterrupted());

            // Clear interrupt flag for other tests
            Thread.interrupted();
        }

        @Test
        void givenIOException_whenSend_thenThrowsArkException()
                throws IOException, InterruptedException {
            HttpClient mockClient = mock(HttpClient.class);
            when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new IOException("connection refused"));

            ArkJdkHttpTransport transport = new ArkJdkHttpTransport(mockClient);

            ArkException ex = assertThrows(ArkException.class, () ->
                    transport.send("GET", URI.create("http://localhost/test"), Map.of(), null, null));

            assertTrue(ex.getMessage().contains("connection refused"));
            assertInstanceOf(IOException.class, ex.getCause());
        }
    }

    @Nested
    class ImplementsInterfaces {

        @Test
        void givenTransport_whenChecked_thenImplementsHttpTransport() {
            assertInstanceOf(HttpTransport.class, transport());
        }

        @Test
        void givenTransport_whenChecked_thenImplementsAsyncHttpTransport() {
            assertInstanceOf(AsyncHttpTransport.class, transport());
        }
    }
}