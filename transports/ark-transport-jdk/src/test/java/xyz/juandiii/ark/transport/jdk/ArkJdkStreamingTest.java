package xyz.juandiii.ark.transport.jdk;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import xyz.juandiii.ark.core.exceptions.ResponseTooLargeException;
import xyz.juandiii.ark.core.http.StreamingResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArkJdkStreamingTest {

    private static HttpServer server;
    private static URI baseUri;
    private static HttpClient client;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/file", exchange -> {
            exchange.getResponseHeaders().add("ETag", "\"v1\"");
            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().add("Content-Length", "50000");
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }
            byte[] body = new byte[50_000];
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        server.createContext("/chunked", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                byte[] chunk = new byte[1_000];
                for (int i = 0; i < 10; i++) {
                    os.write(chunk);
                    os.flush();
                }
            } catch (IOException ignored) {
                // client aborted
            }
        });

        server.createContext("/not-modified", exchange -> {
            exchange.getResponseHeaders().add("ETag", "\"v1\"");
            exchange.getResponseHeaders().add("Content-Length", "50000");
            exchange.sendResponseHeaders(304, -1);
            exchange.close();
        });

        server.createContext("/slow-big", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(new byte[1_000]);
                os.flush();
                Thread.sleep(200);
                os.write(new byte[100_000]);
            } catch (IOException | InterruptedException ignored) {
                // client aborted
            }
        });

        server.start();
        baseUri = URI.create("http://localhost:" + server.getAddress().getPort());
        client = HttpClient.newHttpClient();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    private static URI uri(String path) {
        return baseUri.resolve(path);
    }

    @Test
    void stream_readsAllBytes() throws Exception {
        ArkJdkSyncTransport transport = new ArkJdkSyncTransport(client);

        try (StreamingResponse r = transport.sendStreaming("GET", uri("/file"), Map.of(), null, null)) {
            assertEquals(200, r.statusCode());
            assertTrue(r.header("etag").isPresent());
            assertEquals(50_000, r.body().readAllBytes().length);
        }
    }

    @Test
    void stream_knownLengthOverLimit_rejectedBeforeReturning() {
        ArkJdkSyncTransport transport = new ArkJdkSyncTransport(client, 1_000);

        ResponseTooLargeException ex = assertThrows(ResponseTooLargeException.class,
                () -> transport.sendStreaming("GET", uri("/file"), Map.of(), null, null));

        assertEquals(1_000, ex.limit());
        assertEquals(uri("/file"), ex.uri());
    }

    @Test
    void stream_chunkedOverLimit_throwsWhileReading() throws Exception {
        ArkJdkSyncTransport transport = new ArkJdkSyncTransport(client, 5_000);

        try (StreamingResponse r = transport.sendStreaming("GET", uri("/chunked"), Map.of(), null, null)) {
            InputStream body = r.body();
            assertThrows(ResponseTooLargeException.class, body::readAllBytes);
        }
    }

    @Test
    void stream_underLimit_passes() throws Exception {
        ArkJdkSyncTransport transport = new ArkJdkSyncTransport(client, 100_000);

        try (StreamingResponse r = transport.sendStreaming("GET", uri("/file"), Map.of(), null, null)) {
            assertEquals(50_000, r.body().readAllBytes().length);
        }
    }

    @Test
    void stream_304_emptyBodyNoThrow() throws Exception {
        ArkJdkSyncTransport transport = new ArkJdkSyncTransport(client, 1_000);

        try (StreamingResponse r = transport.sendStreaming("GET", uri("/not-modified"), Map.of(), null, null)) {
            assertEquals(304, r.statusCode());
            assertEquals(0, r.body().readAllBytes().length);
        }
    }

    @Test
    void stream_closeBeforeEof_doesNotHang() {
        ArkJdkSyncTransport transport = new ArkJdkSyncTransport(client);

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            StreamingResponse r = transport.sendStreaming("GET", uri("/slow-big"), Map.of(), null, null);
            r.body().readNBytes(100);
            r.close();
        });

        try (StreamingResponse again = transport.sendStreaming("GET", uri("/file"), Map.of(), null, null)) {
            assertEquals(200, again.statusCode());
        }
    }

    @Test
    void stream_headRequest_noBody() throws Exception {
        ArkJdkSyncTransport transport = new ArkJdkSyncTransport(client, 1_000);

        try (StreamingResponse r = transport.sendStreaming("HEAD", uri("/file"), Map.of(), null, null)) {
            assertEquals(200, r.statusCode());
            assertEquals(0, r.body().readAllBytes().length);
        }
    }
}
