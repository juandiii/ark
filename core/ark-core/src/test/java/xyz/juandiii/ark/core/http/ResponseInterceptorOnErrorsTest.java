package xyz.juandiii.ark.core.http;

import org.junit.jupiter.api.Test;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.TypeRef;
import xyz.juandiii.ark.core.exceptions.NotFoundException;
import xyz.juandiii.ark.core.exceptions.UnauthorizedException;
import xyz.juandiii.ark.core.interceptor.ResponseInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression guard for plan 013: the framework — not the transport — is the
 * single place HTTP errors become exceptions. {@link ResponseInterceptor}s
 * therefore see ALL responses including 4xx / 5xx and can inspect, transform,
 * or recover before {@code validateResponse()} throws.
 */
class ResponseInterceptorOnErrorsTest {

    private static final JsonSerializer NOOP_SERIALIZER = new JsonSerializer() {
        @Override public String serialize(Object body) { return null; }
        @Override public <T> T deserialize(String json, TypeRef<T> type) { return null; }
    };

    private DefaultClientRequest request(HttpTransport transport, List<ResponseInterceptor> interceptors) {
        return new DefaultClientRequest("GET", "https://api.example.com", "/x",
                transport, NOOP_SERIALIZER, List.of(), interceptors);
    }

    @Test
    void givenErrorResponse_whenInterceptorRegistered_thenInterceptorSeesIt() {
        HttpTransport transport = (m, u, h, b, t) -> new RawResponse(401, Map.of(), "denied");
        AtomicReference<RawResponse> seen = new AtomicReference<>();
        ResponseInterceptor capture = raw -> {
            seen.set(raw);
            return raw;
        };

        assertThrows(UnauthorizedException.class,
                () -> request(transport, List.of(capture)).retrieve());

        assertNotNull(seen.get(), "interceptor must see the error response before framework throws");
        assertEquals(401, seen.get().statusCode());
        assertTrue(seen.get().isError());
    }

    @Test
    void givenInterceptorReplacesErrorWithSuccess_thenFrameworkDoesNotThrow() {
        HttpTransport transport = (m, u, h, b, t) -> new RawResponse(404, Map.of(), "missing");
        ResponseInterceptor recover = raw ->
                raw.isError() ? new RawResponse(200, Map.of(), "recovered") : raw;

        ClientResponse response = assertDoesNotThrow(
                () -> request(transport, List.of(recover)).retrieve());

        assertNotNull(response);
    }

    @Test
    void givenMultipleInterceptors_whenErrorResponse_thenAllRunInOrder() {
        HttpTransport transport = (m, u, h, b, t) -> new RawResponse(500, Map.of(), "boom");
        List<Integer> calledWith = new ArrayList<>();
        ResponseInterceptor first = raw -> {
            calledWith.add(raw.statusCode());
            return raw;
        };
        ResponseInterceptor second = raw -> {
            calledWith.add(raw.statusCode());
            return raw;
        };

        assertThrows(Throwable.class,
                () -> request(transport, List.of(first, second)).retrieve());

        assertEquals(List.of(500, 500), calledWith,
                "both interceptors must see the error response in registration order");
    }

    @Test
    void givenSuccessResponse_thenFrameworkDoesNotThrow() {
        HttpTransport transport = (m, u, h, b, t) -> new RawResponse(200, Map.of(), "ok");
        AtomicReference<RawResponse> seen = new AtomicReference<>();
        ResponseInterceptor capture = raw -> {
            seen.set(raw);
            return raw;
        };

        ClientResponse response = request(transport, List.of(capture)).retrieve();

        assertNotNull(response);
        assertNotNull(seen.get());
        assertEquals(200, seen.get().statusCode());
    }

    @Test
    void givenErrorResponse_whenNoInterceptors_thenFrameworkThrowsMatchingType() {
        HttpTransport transport = (m, u, h, b, t) -> new RawResponse(404, Map.of(), "not found");

        assertThrows(NotFoundException.class,
                () -> request(transport, List.of()).retrieve());
    }
}
