package xyz.juandiii.ark.reactor;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.reactor.http.ReactorClientRequest;
import xyz.juandiii.ark.reactor.http.ReactorHttpTransport;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.RetryPolicy;
import xyz.juandiii.ark.core.http.decorator.Retry;
import xyz.juandiii.ark.reactor.http.decorator.ReactorRetryOps;

import java.time.Duration;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactorArkClientTest {

    @Mock
    JsonSerializer serializer;

    @Mock
    ReactorHttpTransport transport;

    private ReactorArk client() {
        return ReactorArkClient.builder()
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
                    ReactorArkClient.builder().transport(transport).build());
        }

        @Test
        void givenNullTransport_whenBuild_thenThrowsNullPointerException() {
            assertThrows(NullPointerException.class, () ->
                    ReactorArkClient.builder().serializer(serializer).build());
        }

        @Test
        void givenValidConfig_whenBuild_thenReturnsReactorArk() {
            assertNotNull(client());
            assertInstanceOf(ReactorArk.class, client());
        }

        @Test
        void givenRetryWrappedTransport_whenRetrieve_thenRetriesThroughBuilder() {
            when(transport.send(anyString(), any(), any(), any(), any()))
                    .thenReturn(Mono.just(new RawResponse(503, Map.of(), "down")))
                    .thenReturn(Mono.just(new RawResponse(200, Map.of(), "ok")));
            RetryPolicy policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(1)).build();

            ReactorArk retrying = ReactorArkClient.builder()
                    .serializer(serializer)
                    .transport(Retry.of(policy, new ReactorRetryOps()).apply(transport))
                    .baseUrl("https://api.example.com")
                    .build();

            assertEquals(200, retrying.get("/status").retrieve().raw().block().statusCode());
            verify(transport, times(2)).send(anyString(), any(), any(), any(), any());
        }
    }

    @Nested
    class HttpMethods {

        @Test
        void givenClient_whenGet_thenReturnsReactorClientRequest() {
            assertNotNull(client().get("/users"));
            assertInstanceOf(ReactorClientRequest.class, client().get("/users"));
        }

        @Test
        void givenClient_whenPost_thenReturnsReactorClientRequest() {
            assertNotNull(client().post("/users"));
        }

        @Test
        void givenClient_whenPut_thenReturnsReactorClientRequest() {
            assertNotNull(client().put("/users/1"));
        }

        @Test
        void givenClient_whenPatch_thenReturnsReactorClientRequest() {
            assertNotNull(client().patch("/users/1"));
        }

        @Test
        void givenClient_whenDelete_thenReturnsReactorClientRequest() {
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
                    .thenReturn(Mono.just(new RawResponse(200, Map.of(), "{}")));

            ReactorArk ark = ReactorArkClient.builder()
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