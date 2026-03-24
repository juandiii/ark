package xyz.juandiii.ark.async;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.async.http.AsyncClientRequest;
import xyz.juandiii.ark.async.http.AsyncHttpTransport;
import xyz.juandiii.ark.http.RawResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncArkClientTest {

    @Mock
    JsonSerializer serializer;

    @Mock
    AsyncHttpTransport transport;

    private AsyncArk client() {
        return AsyncArkClient.builder()
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
                    AsyncArkClient.builder().transport(transport).build());
        }

        @Test
        void givenNullTransport_whenBuild_thenThrowsNullPointerException() {
            assertThrows(NullPointerException.class, () ->
                    AsyncArkClient.builder().serializer(serializer).build());
        }

        @Test
        void givenValidConfig_whenBuild_thenReturnsAsyncArk() {
            assertNotNull(client());
            assertInstanceOf(AsyncArk.class, client());
        }
    }

    @Nested
    class HttpMethods {

        @Test
        void givenClient_whenGet_thenReturnsAsyncClientRequest() {
            assertNotNull(client().get("/users"));
            assertInstanceOf(AsyncClientRequest.class, client().get("/users"));
        }

        @Test
        void givenClient_whenPost_thenReturnsAsyncClientRequest() {
            assertNotNull(client().post("/users"));
        }

        @Test
        void givenClient_whenPut_thenReturnsAsyncClientRequest() {
            assertNotNull(client().put("/users/1"));
        }

        @Test
        void givenClient_whenPatch_thenReturnsAsyncClientRequest() {
            assertNotNull(client().patch("/users/1"));
        }

        @Test
        void givenClient_whenDelete_thenReturnsAsyncClientRequest() {
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
            when(transport.sendAsync(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(
                            new RawResponse(200, Map.of(), "{}")));

            AsyncArk ark = AsyncArkClient.builder()
                    .serializer(serializer)
                    .transport(transport)
                    .userAgent("TestApp", "1.0")
                    .baseUrl("https://api.example.com")
                    .build();

            ark.get("/").retrieve();

            verify(transport).sendAsync(anyString(), any(), argThat(headers ->
                    "TestApp/1.0".equals(headers.get("User-Agent"))), any(), any());
        }
    }
}
