package xyz.juandiii.ark.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.interceptor.RequestContext;

import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RequestContextTest {

    @Mock
    JsonSerializer serializer;

    private DefaultClientRequest request() {
        HttpTransport transport = (m, uri, headers, body, timeout) ->
                new RawResponse(200, java.util.Map.of(), "{}");
        return new DefaultClientRequest("POST", "https://api.example.com", "/users",
                transport, serializer, Collections.emptyList(), Collections.emptyList());
    }

    @Test
    void methodReturnsHttpMethod() {
        assertEquals("POST", request().method());
    }

    @Test
    void pathReturnsFullPath() {
        assertEquals("https://api.example.com/users", request().path());
    }

    @Test
    void headersReturnsUnmodifiableMap() {
        DefaultClientRequest req = request();
        req.header("X-Key", "value");
        assertThrows(UnsupportedOperationException.class,
                () -> req.headers().put("another", "value"));
    }

    @Test
    void queryParamsReturnsUnmodifiableMap() {
        DefaultClientRequest req = request();
        req.queryParam("key", "value");
        assertThrows(UnsupportedOperationException.class,
                () -> req.queryParams().put("another", "value"));
    }

    @Test
    void bodyReturnsSetBody() {
        DefaultClientRequest req = request();
        req.body("payload");
        assertEquals("payload", req.body());
    }

    @Test
    void bodyDefaultsToNull() {
        assertNull(request().body());
    }

    @Test
    void timeoutReturnsSetTimeout() {
        DefaultClientRequest req = request();
        req.timeout(Duration.ofSeconds(30));
        assertEquals(Duration.ofSeconds(30), req.timeout());
    }

    @Test
    void timeoutDefaultsToNull() {
        assertNull(request().timeout());
    }

    @Test
    void implementsRequestContext() {
        assertInstanceOf(RequestContext.class, request());
    }
}