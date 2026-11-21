package xyz.juandiii.ark.quarkus.vertx.deployment;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import xyz.juandiii.ark.core.exceptions.TimeoutException;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.proxy.RegisterArkClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArkVertxRecorderTest {

    static final HttpServer server;
    static final AtomicInteger flakyHits = new AtomicInteger();

    static {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/flaky", ex -> {
            flakyHits.incrementAndGet();
            ex.sendResponseHeaders(503, -1);
            ex.close();
        });
        // QuarkusUnitTest loads this class twice (JUnit and Quarkus classloaders), so this block
        // starts two servers. The clients use the one in the config; the test reads its count
        // over HTTP instead of from this field.
        server.createContext("/hits", ex -> write(ex, String.valueOf(flakyHits.getAndSet(0))));
        server.createContext("/redirect", ex -> {
            ex.getResponseHeaders().add("Location", "/ok");
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });
        server.createContext("/ok", ex -> write(ex, "ok"));
        server.createContext("/slow", ex -> {
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            write(ex, "late");
        });
        server.start();
    }

    private static void write(HttpExchange ex, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Path("/")
    @RegisterArkClient(configKey = "retrying")
    public interface RetryingApi {
        @GET
        @Path("/flaky")
        Uni<RawResponse> flaky();
    }

    @Path("/")
    @RegisterArkClient(configKey = "no-redirects")
    public interface NoRedirectsApi {
        @GET
        @Path("/redirect")
        Uni<RawResponse> redirect();
    }

    @Path("/")
    @RegisterArkClient(configKey = "defaults")
    public interface DefaultsApi {
        @GET
        @Path("/redirect")
        Uni<RawResponse> redirect();

        @GET
        @Path("/flaky")
        Uni<RawResponse> flaky();

        @GET
        @Path("/slow")
        Uni<RawResponse> slow();
    }

    static final String BASE = "http://localhost:" + server.getAddress().getPort();

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(RetryingApi.class, NoRedirectsApi.class, DefaultsApi.class))
            .overrideConfigKey("ark.client.\"retrying\".base-url", BASE)
            .overrideConfigKey("ark.client.\"retrying\".http-version", "HTTP_1_1")
            .overrideConfigKey("ark.client.\"retrying\".retry.max-attempts", "3")
            .overrideConfigKey("ark.client.\"retrying\".retry.delay", "1")
            .overrideConfigKey("ark.client.\"no-redirects\".base-url", BASE)
            .overrideConfigKey("ark.client.\"no-redirects\".http-version", "HTTP_1_1")
            .overrideConfigKey("ark.client.\"no-redirects\".follow-redirects", "false")
            .overrideConfigKey("ark.client.\"defaults\".base-url", BASE)
            .overrideConfigKey("ark.client.\"defaults\".http-version", "HTTP_1_1")
            .overrideConfigKey("ark.client.\"defaults\".read-timeout", "1");

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Inject
    RetryingApi retryingApi;

    @Inject
    NoRedirectsApi noRedirectsApi;

    @Inject
    DefaultsApi defaultsApi;

    private static final Duration WAIT = Duration.ofSeconds(10);

    /** Returns the /flaky hit count on the server the clients use, and resets it. */
    private static int takeFlakyHits() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(ConfigProvider.getConfig()
                        .getValue("ark.client.\"defaults\".base-url", String.class) + "/hits")).build(),
                HttpResponse.BodyHandlers.ofString());
        return Integer.parseInt(response.body());
    }

    @Test
    void retriesWhenMaxAttemptsIsAboveOne() throws Exception {
        takeFlakyHits();
        retryingApi.flaky().onFailure().recoverWithNull().await().atMost(WAIT);
        assertEquals(3, takeFlakyHits());
    }

    @Test
    void doesNotRetryByDefault() throws Exception {
        takeFlakyHits();
        defaultsApi.flaky().onFailure().recoverWithNull().await().atMost(WAIT);
        assertEquals(1, takeFlakyHits());
    }

    @Test
    void followsRedirectsByDefault() {
        assertEquals(200, defaultsApi.redirect().await().atMost(WAIT).statusCode());
    }

    @Test
    void returnsRedirectWhenFollowRedirectsIsFalse() {
        assertEquals(302, noRedirectsApi.redirect().await().atMost(WAIT).statusCode());
    }

    @Test
    void readTimeoutFailsWithTimeoutException() {
        assertThrows(TimeoutException.class, () -> defaultsApi.slow().await().atMost(WAIT));
    }
}
