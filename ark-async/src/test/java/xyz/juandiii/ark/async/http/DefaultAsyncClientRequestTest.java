package xyz.juandiii.ark.async.http;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.exceptions.ApiException;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.interceptor.RequestInterceptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultAsyncClientRequestTest {

    @Mock
    AsyncHttpTransport transport;

    @Mock
    JsonSerializer serializer;

    private DefaultAsyncClientRequest request(String method, String path) {
        return new DefaultAsyncClientRequest(method, "https://api.example.com", path,
                transport, serializer, Collections.emptyList(), Collections.emptyList());
    }

    @Nested
    class Retrieve {

        @Test
        void givenSuccessResponse_whenRetrieve_thenReturnsAsyncClientResponse() {
            when(transport.sendAsync(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(
                            new RawResponse(200, Map.of(), "{}")));

            AsyncClientResponse response = request("GET", "/users").retrieve();

            assertNotNull(response);
        }

        @Test
        void givenErrorResponse_whenRetrieve_thenFutureFailsWithApiException() {
            when(transport.sendAsync(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(
                            new RawResponse(500, Map.of(), "Server Error")));

            AsyncClientResponse response = request("GET", "/fail").retrieve();

            ExecutionException ex = assertThrows(ExecutionException.class,
                    () -> response.body(String.class).get());
            assertInstanceOf(ApiException.class, ex.getCause());
            assertEquals(500, ((ApiException) ex.getCause()).statusCode());
        }

        @Test
        void givenRequestInterceptor_whenRetrieve_thenInterceptorRunsBeforeSend() {
            RequestInterceptor interceptor = mock(RequestInterceptor.class);
            when(transport.sendAsync(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(
                            new RawResponse(200, Map.of(), "{}")));

            DefaultAsyncClientRequest req = new DefaultAsyncClientRequest(
                    "GET", "https://api.example.com", "/",
                    transport, serializer, List.of(interceptor), Collections.emptyList());
            req.retrieve();

            verify(interceptor).intercept(req);
        }

        @Test
        void givenResponseInterceptor_whenRetrieve_thenInterceptorChainsViaThenApply() throws Exception {
            when(transport.sendAsync(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(
                            new RawResponse(200, Map.of(), "original")));

            DefaultAsyncClientRequest req = new DefaultAsyncClientRequest(
                    "GET", "https://api.example.com", "/",
                    transport, serializer, Collections.emptyList(),
                    List.of(raw -> new RawResponse(raw.statusCode(), raw.headers(), "modified")));

            AsyncClientResponse response = req.retrieve();
            // Verify the future completes without error (interceptor applied)
            assertNotNull(response.toBodilessEntity().get());
        }
    }

    @Nested
    class FluentChaining {

        @Test
        void givenRequest_whenChaining_thenReturnsSameInstance() {
            DefaultAsyncClientRequest req = request("GET", "/");
            assertSame(req, req.accept("application/json"));
            assertSame(req, req.contentType("text/plain"));
            assertSame(req, req.header("X-Key", "value"));
            assertSame(req, req.queryParam("k", "v"));
            assertSame(req, req.body("test"));
            assertSame(req, req.timeout(java.time.Duration.ofSeconds(5)));
        }
    }

    @Nested
    class ImplementsInterface {

        @Test
        void givenRequest_whenChecked_thenImplementsAsyncClientRequest() {
            assertInstanceOf(AsyncClientRequest.class, request("GET", "/"));
        }
    }
}