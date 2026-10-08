package xyz.juandiii.ark.quarkus.deployment;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.core.exceptions.ResponseTooLargeException;
import xyz.juandiii.ark.quarkus.ArkClients;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArkClientsTest {

    static final HttpServer server;

    static {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/ok", ex -> write(ex, "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8)));
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

    private static void write(HttpExchange ex, byte[] body) throws IOException {
        ex.sendResponseHeaders(200, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("ark.client.\"src\".base-url", "http://localhost:" + server.getAddress().getPort())
            .overrideConfigKey("ark.client.\"src\".max-response-bytes", "100")
            .overrideConfigKey("ark.client.\"src\".headers.X-Test", "yes");

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Inject
    ArkClients arkClients;

    @Test
    void givenNamedConfig_whenBuilder_thenUsesBaseUrl() {
        int status = arkClients.builder("src").build().get("/ok").retrieve().raw().statusCode();
        assertEquals(200, status);
    }

    @Test
    void givenMaxResponseBytes_whenBodyTooLarge_thenThrowsResponseTooLarge() {
        var client = arkClients.builder("src").build();
        assertThrows(ResponseTooLargeException.class, () -> client.get("/big").retrieve().raw());
    }

    @Test
    void givenNamedConfig_whenRetrieveStream_thenAppliesLimitWhileReading() {
        var client = arkClients.builder("src").build();
        // Both outcomes are valid: with a Content-Length the limit trips before the response
        // is returned; for a chunked body it trips while the stream is being read.
        boolean observed = false;
        try (var response = client.get("/big").noThrow().retrieveStream()) {
            response.body().readAllBytes();
        } catch (ResponseTooLargeException e) {
            observed = true;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        assertTrue(observed, "expected ResponseTooLargeException");
    }

    @Test
    void givenNamedConfig_whenBuilder_thenAppliesHeaders() {
        String body = arkClients.builder("src").build().get("/echo-header").retrieve().body(String.class);
        assertEquals("yes", body);
    }

    @Test
    void givenUnknownKey_whenBuilder_thenThrowsArkException() {
        assertThrows(ArkException.class, () -> arkClients.builder("missing"));
    }

    @Test
    void givenTwoCalls_whenBuilder_thenDifferentInstances() {
        assertNotSame(arkClients.builder("src"), arkClients.builder("src"));
    }
}
