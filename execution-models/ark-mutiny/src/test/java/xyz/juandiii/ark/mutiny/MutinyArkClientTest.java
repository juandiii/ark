package xyz.juandiii.ark.mutiny;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.RetryPolicy;
import xyz.juandiii.ark.core.http.decorator.Retry;
import xyz.juandiii.ark.mutiny.http.decorator.MutinyRetryOps;

import java.time.Duration;
import xyz.juandiii.ark.mutiny.http.MutinyClientRequest;
import xyz.juandiii.ark.mutiny.http.MutinyHttpTransport;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MutinyArkClientTest {

    @Mock
    JsonSerializer serializer;

    @Mock
    MutinyHttpTransport transport;

    private MutinyArk client() {
        return MutinyArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .build();
    }

    @Nested
    class Builder {

        @Test
        void givenNullSerializer_whenBuild_thenThrowsNullPointerException() {
            assertThrows(NullPointerException.class, () ->
                    MutinyArkClient.builder().transport(transport).build());
        }

        @Test
        void givenNullTransport_whenBuild_thenThrowsNullPointerException() {
            assertThrows(NullPointerException.class, () ->
                    MutinyArkClient.builder().serializer(serializer).build());
        }

        @Test
        void givenValidConfig_whenBuild_thenReturnsMutinyArk() {
            assertNotNull(client());
            assertInstanceOf(MutinyArk.class, client());
        }

        @Test
        void givenRetryWrappedTransport_whenRetrieve_thenRetriesThroughBuilder() {
            when(transport.send(anyString(), any(), any(), any(), any()))
                    .thenReturn(Uni.createFrom().item(new RawResponse(503, Map.of(), "down")))
                    .thenReturn(Uni.createFrom().item(new RawResponse(200, Map.of(), "ok")));
            RetryPolicy policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(1)).build();

            MutinyArk retrying = MutinyArkClient.builder()
                    .serializer(serializer)
                    .transport(Retry.of(policy, new MutinyRetryOps()).apply(transport))
                    .baseUrl("https://api.example.com")
                    .build();

            assertEquals(200, retrying.get("/status").retrieve().raw().await().indefinitely().statusCode());
            verify(transport, times(2)).send(anyString(), any(), any(), any(), any());
        }
    }

    @Nested
    class HttpMethods {

        @Test
        void givenClient_whenGet_thenReturnsMutinyClientRequest() {
            assertNotNull(client().get("/users"));
            assertInstanceOf(MutinyClientRequest.class, client().get("/users"));
        }

        @Test
        void givenClient_whenPost_thenReturnsMutinyClientRequest() {
            assertNotNull(client().post("/users"));
        }

        @Test
        void givenClient_whenPut_thenReturnsMutinyClientRequest() {
            assertNotNull(client().put("/users/1"));
        }

        @Test
        void givenClient_whenPatch_thenReturnsMutinyClientRequest() {
            assertNotNull(client().patch("/users/1"));
        }

        @Test
        void givenClient_whenDelete_thenReturnsMutinyClientRequest() {
            assertNotNull(client().delete("/users/1"));
        }

        @Test
        void givenClient_whenGetWithoutPath_thenDefaultsToRoot() {
            assertNotNull(client().get());
        }
    }

    @Nested
    class UserAgent {

        @Test
        void givenCustomUserAgent_whenRetrieve_thenSendsUserAgentHeader() {
            when(transport.send(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(Uni.createFrom().item(new RawResponse(200, Map.of(), "{}")));

            MutinyArk ark = MutinyArkClient.builder()
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