package xyz.juandiii.ark.transport.apache;

import com.sun.net.httpserver.HttpServer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.juandiii.ark.core.exceptions.ApiException;
import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.core.exceptions.ConnectionException;
import xyz.juandiii.ark.core.exceptions.NotFoundException;
import xyz.juandiii.ark.core.exceptions.ServerException;
import xyz.juandiii.ark.core.http.RawResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArkApacheTransportTest {

    private static HttpServer server;
    private static URI baseUri;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/ok", exchange -> {
            exchange.getResponseHeaders().add("X-Custom", "apache-value");
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

        server.createContext("/echo-content-type", exchange -> {
            String ct = exchange.getRequestHeaders().getFirst("Content-Type");
            byte[] body = (ct != null ? ct : "none").getBytes();
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
    }

    private ArkApacheTransport transport() {
        return new ArkApacheTransport(HttpClients.createDefault());
    }

    @Nested
    class Constructor {

        @Test
        void givenNullHttpClient_whenConstructing_thenThrowsNullPointerException() {
            assertThrows(NullPointerException.class, () -> new ArkApacheTransport(null));
        }

        @Test
        void givenValidHttpClient_whenConstructing_thenSucceeds() {
            assertDoesNotThrow(() -> new ArkApacheTransport(HttpClients.createDefault()));
        }

        @Test
        void givenCustomPoolConfig_whenConstructing_thenSucceeds() {
            CloseableHttpClient client = HttpClients.custom()
                    .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                            .setMaxConnTotal(50)
                            .setMaxConnPerRoute(10)
                            .build())
                    .build();
            assertDoesNotThrow(() -> new ArkApacheTransport(client));
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
        void givenSuccessEndpoint_whenSend_thenResponseContainsHeaders() {
            RawResponse response = transport().send("GET", baseUri.resolve("/ok"),
                    Map.of(), null, null);

            boolean hasHeader = response.headers().keySet().stream()
                    .anyMatch(k -> k.equalsIgnoreCase("X-Custom"));
            assertTrue(hasHeader);
        }

        @Test
        void given404Endpoint_whenSend_thenThrowsApiException() {
            ApiException ex = assertThrows(ApiException.class, () ->
                    transport().send("GET", baseUri.resolve("/not-found"), Map.of(), null, null));

            assertEquals(404, ex.statusCode());
            assertEquals("Not Found", ex.responseBody());
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
                    Map.of("Authorization", "Bearer apache-token"), null, null);

            assertEquals("Bearer apache-token", response.body());
        }

        @Test
        void givenJsonBody_whenSendWithoutContentType_thenDefaultsToApplicationJson() {
            RawResponse response = transport().send("POST", baseUri.resolve("/echo-content-type"),
                    Map.of(), "{\"data\":true}", null);

            assertTrue(response.body().contains("application/json"));
        }

        @Test
        void givenFormBody_whenSendWithFormContentType_thenUsesFormContentType() {
            RawResponse response = transport().send("POST", baseUri.resolve("/echo-content-type"),
                    Map.of("Content-Type", "application/x-www-form-urlencoded"),
                    "key=value", null);

            assertTrue(response.body().contains("application/x-www-form-urlencoded"));
        }

        @Test
        void givenTimeout_whenSlowEndpoint_thenThrowsArkException() {
            assertThrows(ArkException.class, () ->
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
        void givenSlowHeaders_whenSendWithTimeout_thenThrowsArkException() throws Exception {
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

                assertThrows(ArkException.class, () ->
                        transport().send("GET", URI.create("http://localhost:" + port + "/slow-headers"),
                                Map.of(), null, Duration.ofMillis(200)));
            }
        }
    }

    @Nested
    class ExceptionHandling {

        @Test
        void givenIOException_whenSend_thenThrowsArkException() throws IOException {
            CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
            when(mockClient.execute(
                    any(org.apache.hc.core5.http.ClassicHttpRequest.class),
                    any(org.apache.hc.core5.http.protocol.HttpContext.class),
                    any(org.apache.hc.core5.http.io.HttpClientResponseHandler.class)))
                    .thenThrow(new IOException("connection refused"));

            ArkApacheTransport transport = new ArkApacheTransport(mockClient);

            ArkException ex = assertThrows(ArkException.class, () ->
                    transport.send("GET", URI.create("http://localhost/test"), Map.of(), null, null));

            assertTrue(ex.getMessage().contains("connection refused"));
            assertInstanceOf(IOException.class, ex.getCause());
        }
    }

    @Nested
    class ImplementsInterface {

        @Test
        void givenTransport_whenChecked_thenImplementsHttpTransport() {
            assertInstanceOf(xyz.juandiii.ark.core.http.HttpTransport.class, transport());
        }
    }
}