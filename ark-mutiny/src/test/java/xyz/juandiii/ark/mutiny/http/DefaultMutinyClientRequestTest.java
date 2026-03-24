package xyz.juandiii.ark.mutiny.http;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultMutinyClientRequestTest {

    @Mock
    MutinyHttpTransport transport;

    @Mock
    JsonSerializer serializer;

    private DefaultMutinyClientRequest request(String method, String path) {
        return new DefaultMutinyClientRequest(method, "https://api.example.com", path,
                transport, serializer, Collections.emptyList(), Collections.emptyList());
    }

    @Nested
    class Retrieve {

        @Test
        void givenSuccessResponse_whenRetrieve_thenReturnsMutinyClientResponse() {
            when(transport.send(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(Uni.createFrom().item(new RawResponse(200, Map.of(), "{}")));

            MutinyClientResponse response = request("GET", "/users").retrieve();

            assertNotNull(response);
        }

        @Test
        void givenErrorResponse_whenRetrieve_thenUniFailsWithApiException() {
            when(transport.send(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(Uni.createFrom().item(new RawResponse(500, Map.of(), "Server Error")));

            MutinyClientResponse response = request("GET", "/fail").retrieve();

            UniAssertSubscriber<String> subscriber = response.body(String.class)
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertFailedWith(ApiException.class);
        }

        @Test
        void givenRequestInterceptor_whenRetrieve_thenInterceptorRunsBeforeSend() {
            RequestInterceptor interceptor = mock(RequestInterceptor.class);
            when(transport.send(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(Uni.createFrom().item(new RawResponse(200, Map.of(), "{}")));

            DefaultMutinyClientRequest req = new DefaultMutinyClientRequest(
                    "GET", "https://api.example.com", "/",
                    transport, serializer, List.of(interceptor), Collections.emptyList());
            req.retrieve();

            verify(interceptor).intercept(req);
        }

        @Test
        void givenResponseInterceptor_whenRetrieve_thenInterceptorChainsViaTransform() {
            when(transport.send(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(Uni.createFrom().item(new RawResponse(200, Map.of(), "original")));

            DefaultMutinyClientRequest req = new DefaultMutinyClientRequest(
                    "GET", "https://api.example.com", "/",
                    transport, serializer, Collections.emptyList(),
                    List.of(raw -> new RawResponse(raw.statusCode(), raw.headers(), "modified")));

            MutinyClientResponse response = req.retrieve();

            UniAssertSubscriber<Object> subscriber = response.toBodilessEntity()
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertCompleted();
        }
    }

    @Nested
    class FluentChaining {

        @Test
        void givenRequest_whenChaining_thenReturnsSameInstance() {
            DefaultMutinyClientRequest req = request("GET", "/");
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
        void givenRequest_whenChecked_thenImplementsMutinyClientRequest() {
            assertInstanceOf(MutinyClientRequest.class, request("GET", "/"));
        }
    }
}