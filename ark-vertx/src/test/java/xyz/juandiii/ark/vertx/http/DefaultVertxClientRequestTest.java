package xyz.juandiii.ark.vertx.http;

import io.vertx.core.Future;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.exceptions.ApiException;
import xyz.juandiii.ark.http.RawResponse;
import xyz.juandiii.ark.interceptor.RequestInterceptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultVertxClientRequestTest {

    @Mock
    VertxHttpTransport transport;

    @Mock
    JsonSerializer serializer;

    private DefaultVertxClientRequest request(String method, String path) {
        return new DefaultVertxClientRequest(method, "https://api.example.com", path,
                transport, serializer, Collections.emptyList(), Collections.emptyList());
    }

    @Nested
    class Retrieve {

        @Test
        void givenSuccessResponse_whenRetrieve_thenReturnsVertxClientResponse() {
            when(transport.send(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(Future.succeededFuture(new RawResponse(200, Map.of(), "{}")));

            VertxClientResponse response = request("GET", "/users").retrieve();

            assertNotNull(response);
        }

        @Test
        void givenErrorResponse_whenRetrieve_thenFutureFailsWithApiException() throws Exception {
            when(transport.send(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(Future.succeededFuture(new RawResponse(500, Map.of(), "Server Error")));

            VertxClientResponse response = request("GET", "/fail").retrieve();

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> error = new AtomicReference<>();

            response.body(String.class).onComplete(ar -> {
                if (ar.failed()) {
                    error.set(ar.cause());
                }
                latch.countDown();
            });

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertNotNull(error.get());
            assertInstanceOf(ApiException.class, error.get());
            assertEquals(500, ((ApiException) error.get()).statusCode());
        }

        @Test
        void givenRequestInterceptor_whenRetrieve_thenInterceptorRunsBeforeSend() {
            RequestInterceptor interceptor = mock(RequestInterceptor.class);
            when(transport.send(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(Future.succeededFuture(new RawResponse(200, Map.of(), "{}")));

            DefaultVertxClientRequest req = new DefaultVertxClientRequest(
                    "GET", "https://api.example.com", "/",
                    transport, serializer, List.of(interceptor), Collections.emptyList());
            req.retrieve();

            verify(interceptor).intercept(req);
        }

        @Test
        void givenResponseInterceptor_whenRetrieve_thenInterceptorChainsViaMap() throws Exception {
            when(transport.send(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(Future.succeededFuture(new RawResponse(200, Map.of(), "original")));

            DefaultVertxClientRequest req = new DefaultVertxClientRequest(
                    "GET", "https://api.example.com", "/",
                    transport, serializer, Collections.emptyList(),
                    List.of(raw -> new RawResponse(raw.statusCode(), raw.headers(), "modified")));

            VertxClientResponse response = req.retrieve();

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Object> result = new AtomicReference<>();

            response.toBodilessEntity().onComplete(ar -> {
                if (ar.succeeded()) {
                    result.set(ar.result());
                }
                latch.countDown();
            });

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertNotNull(result.get());
        }
    }

    @Nested
    class FluentChaining {

        @Test
        void givenRequest_whenChaining_thenReturnsSameInstance() {
            DefaultVertxClientRequest req = request("GET", "/");
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
        void givenRequest_whenChecked_thenImplementsVertxClientRequest() {
            assertInstanceOf(VertxClientRequest.class, request("GET", "/"));
        }
    }
}