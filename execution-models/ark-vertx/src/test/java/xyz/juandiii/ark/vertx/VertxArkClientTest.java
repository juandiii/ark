package xyz.juandiii.ark.vertx;

import io.vertx.core.Future;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.RetryPolicy;
import xyz.juandiii.ark.core.http.decorator.Retry;
import xyz.juandiii.ark.vertx.http.VertxClientRequest;
import xyz.juandiii.ark.vertx.http.VertxHttpTransport;
import xyz.juandiii.ark.vertx.http.decorator.VertxRetryOps;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VertxArkClientTest {

    @Mock
    JsonSerializer serializer;

    @Mock
    VertxHttpTransport transport;

    private VertxArk client() {
        return VertxArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .build();
    }

    @Nested
    class Builder {

        @Test
        void givenNullTransport_whenBuild_thenThrowsNullPointerException() {
            assertThrows(NullPointerException.class, () ->
                    VertxArkClient.builder().serializer(serializer).build());
        }

        @Test
        void givenNullSerializer_whenBuild_thenUsesDefaultVertxJsonSerializer() {
            VertxArk ark = VertxArkClient.builder().transport(transport).build();
            assertNotNull(ark);
            assertInstanceOf(VertxArk.class, ark);
        }

        @Test
        void givenValidConfig_whenBuild_thenReturnsVertxArk() {
            assertNotNull(client());
            assertInstanceOf(VertxArk.class, client());
        }

        @Test
        void givenRetryWrappedTransport_whenRetrieve_thenRetriesThroughBuilder() throws Exception {
            when(transport.send(anyString(), any(), any(), any(), any()))
                    .thenReturn(Future.succeededFuture(new RawResponse(503, Map.of(), "down")))
                    .thenReturn(Future.succeededFuture(new RawResponse(200, Map.of(), "ok")));
            RetryPolicy policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(1)).build();

            VertxArk retrying = VertxArkClient.builder()
                    .serializer(serializer)
                    .transport(Retry.of(policy, new VertxRetryOps()).apply(transport))
                    .baseUrl("https://api.example.com")
                    .build();

            RawResponse raw = retrying.get("/status").retrieve().raw()
                    .toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
            assertEquals(200, raw.statusCode());
            verify(transport, times(2)).send(anyString(), any(), any(), any(), any());
        }
    }

    @Nested
    class HttpMethods {

        @Test
        void givenClient_whenGet_thenReturnsVertxClientRequest() {
            assertNotNull(client().get("/users"));
            assertInstanceOf(VertxClientRequest.class, client().get("/users"));
        }

        @Test
        void givenClient_whenPost_thenReturnsVertxClientRequest() {
            assertNotNull(client().post("/users"));
        }

        @Test
        void givenClient_whenPut_thenReturnsVertxClientRequest() {
            assertNotNull(client().put("/users/1"));
        }

        @Test
        void givenClient_whenPatch_thenReturnsVertxClientRequest() {
            assertNotNull(client().patch("/users/1"));
        }

        @Test
        void givenClient_whenDelete_thenReturnsVertxClientRequest() {
            assertNotNull(client().delete("/users/1"));
        }

        @Test
        void givenClient_whenGetWithoutPath_thenDefaultsToRoot() {
            assertNotNull(client().get());
        }

        @Test
        void givenClient_whenPostWithoutPath_thenDefaultsToRoot() {
            assertNotNull(client().post());
        }

        @Test
        void givenClient_whenPutWithoutPath_thenDefaultsToRoot() {
            assertNotNull(client().put());
        }

        @Test
        void givenClient_whenPatchWithoutPath_thenDefaultsToRoot() {
            assertNotNull(client().patch());
        }

        @Test
        void givenClient_whenDeleteWithoutPath_thenDefaultsToRoot() {
            assertNotNull(client().delete());
        }
    }

    @Nested
    class UserAgent {

        @Test
        void givenCustomUserAgent_whenRetrieve_thenSendsUserAgentHeader() {
            when(transport.send(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(Future.succeededFuture(new RawResponse(200, Map.of(), "{}")));

            VertxArk ark = VertxArkClient.builder()
                    .serializer(serializer)
                    .transport(transport)
                    .userAgent("TestApp", "1.0")
                    .baseUrl("https://api.example.com")
                    .build();

            ark.get("/").retrieve();

            verify(transport).send(anyString(), any(), argThat(headers ->
                    "TestApp/1.0".equals(headers.get("User-Agent"))), any(), any());
        }
    }
}