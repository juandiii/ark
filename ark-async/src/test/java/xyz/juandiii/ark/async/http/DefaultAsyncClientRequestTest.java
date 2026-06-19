package xyz.juandiii.ark.async.http;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.exceptions.ApiException;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.Transport;
import xyz.juandiii.ark.core.interceptor.RequestInterceptor;
import xyz.juandiii.ark.core.interceptor.ResponseInterceptor;

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
    @SuppressWarnings("unchecked")
    Transport<CompletableFuture<RawResponse>> transport;

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
            when(transport.send(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(
                            new RawResponse(200, Map.of(), "{}")));

            AsyncClientResponse response = request("GET", "/users").retrieve();

            assertNotNull(response);
        }

        @Test
        void givenErrorResponse_whenRetrieve_thenFutureFailsWithApiException() {
            when(transport.send(anyString(), any(), anyMap(), any(), any()))
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
            when(transport.send(anyString(), any(), anyMap(), any(), any()))
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
            when(transport.send(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(
                            new RawResponse(200, Map.of(), "original")));

            ResponseInterceptor responseInterceptor =
                    raw -> new RawResponse(raw.statusCode(), raw.headers(), "modified");

            DefaultAsyncClientRequest req = new DefaultAsyncClientRequest(
                    "GET", "https://api.example.com", "/",
                    transport, serializer, Collections.emptyList(),
                    List.of(responseInterceptor));

            AsyncClientResponse response = req.retrieve();
            assertNotNull(response.toBodilessEntity().get());
        }
    }

    @Nested
    class FluentChaining {

        @Test
        void givenRequest_whenChained_thenReturnsSameInstance() {
            DefaultAsyncClientRequest req = request("POST", "/users");
            assertSame(req, req.header("X-Custom", "value")
                    .queryParam("filter", "active")
                    .body("{}")
                    .contentType("application/json")
                    .accept("application/json"));
        }
    }
}
