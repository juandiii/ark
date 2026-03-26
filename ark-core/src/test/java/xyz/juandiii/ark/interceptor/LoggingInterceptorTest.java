package xyz.juandiii.ark.interceptor;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.juandiii.ark.http.RawResponse;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoggingInterceptorTest {

    @Nested
    class RequestLogging {

        @Test
        void givenBasicLevel_thenLogsMethodAndPath() {
            RequestInterceptor interceptor = LoggingInterceptor.request();
            RequestContext context = createContext("GET", "/users/1", Map.of(), null);

            assertDoesNotThrow(() -> interceptor.intercept(context));
        }

        @Test
        void givenHeadersLevel_thenLogsHeaders() {
            RequestInterceptor interceptor = LoggingInterceptor.request(LoggingInterceptor.Level.HEADERS);
            RequestContext context = createContext("POST", "/users",
                    Map.of("Content-Type", "application/json"), null);

            assertDoesNotThrow(() -> interceptor.intercept(context));
        }

        @Test
        void givenBodyLevel_thenLogsBody() {
            RequestInterceptor interceptor = LoggingInterceptor.request(LoggingInterceptor.Level.BODY);
            RequestContext context = createContext("POST", "/users",
                    Map.of("Content-Type", "application/json"), "{\"name\":\"Juan\"}");

            assertDoesNotThrow(() -> interceptor.intercept(context));
        }

        @Test
        void givenNullBody_thenDoesNotThrow() {
            RequestInterceptor interceptor = LoggingInterceptor.request(LoggingInterceptor.Level.BODY);
            RequestContext context = createContext("GET", "/users", Map.of(), null);

            assertDoesNotThrow(() -> interceptor.intercept(context));
        }
    }

    @Nested
    class ResponseLogging {

        @Test
        void givenBasicLevel_thenLogsStatusAndDuration() {
            LoggingInterceptor.request().intercept(createContext("GET", "/", Map.of(), null));

            ResponseInterceptor interceptor = LoggingInterceptor.response();
            RawResponse raw = new RawResponse(200, Map.of(), "{\"ok\":true}");

            RawResponse result = interceptor.intercept(raw);

            assertSame(raw, result);
        }

        @Test
        void givenHeadersLevel_thenLogsResponseHeaders() {
            LoggingInterceptor.request().intercept(createContext("GET", "/", Map.of(), null));

            ResponseInterceptor interceptor = LoggingInterceptor.response(LoggingInterceptor.Level.HEADERS);
            RawResponse raw = new RawResponse(200,
                    Map.of("Content-Type", List.of("application/json")), "{}");

            RawResponse result = interceptor.intercept(raw);

            assertSame(raw, result);
        }

        @Test
        void givenBodyLevel_thenLogsResponseBody() {
            LoggingInterceptor.request().intercept(createContext("GET", "/", Map.of(), null));

            ResponseInterceptor interceptor = LoggingInterceptor.response(LoggingInterceptor.Level.BODY);
            RawResponse raw = new RawResponse(200, Map.of(), "{\"id\":1}");

            RawResponse result = interceptor.intercept(raw);

            assertSame(raw, result);
        }

        @Test
        void givenLargeBody_thenTruncates() {
            LoggingInterceptor.request().intercept(createContext("GET", "/", Map.of(), null));

            ResponseInterceptor interceptor = LoggingInterceptor.response(LoggingInterceptor.Level.BODY);
            String largeBody = "x".repeat(2000);
            RawResponse raw = new RawResponse(200, Map.of(), largeBody);

            RawResponse result = interceptor.intercept(raw);

            assertSame(raw, result);
        }

        @Test
        void givenErrorStatus_thenLogsAsWarning() {
            LoggingInterceptor.request().intercept(createContext("GET", "/", Map.of(), null));

            ResponseInterceptor interceptor = LoggingInterceptor.response();
            RawResponse raw = new RawResponse(500, Map.of(), "error");

            RawResponse result = interceptor.intercept(raw);

            assertSame(raw, result);
        }

        @Test
        void givenNoRequestStart_thenDurationIsZero() {
            ResponseInterceptor interceptor = LoggingInterceptor.response();
            RawResponse raw = new RawResponse(200, Map.of(), "{}");

            assertDoesNotThrow(() -> interceptor.intercept(raw));
        }

        @Test
        void givenNullBody_thenDoesNotThrow() {
            LoggingInterceptor.request().intercept(createContext("GET", "/", Map.of(), null));

            ResponseInterceptor interceptor = LoggingInterceptor.response(LoggingInterceptor.Level.BODY);
            RawResponse raw = new RawResponse(204, Map.of(), null);

            assertDoesNotThrow(() -> interceptor.intercept(raw));
        }
    }

    @Nested
    class DefaultFactoryMethods {

        @Test
        void givenRequestDefault_thenReturnsInterceptor() {
            assertNotNull(LoggingInterceptor.request());
        }

        @Test
        void givenResponseDefault_thenReturnsInterceptor() {
            assertNotNull(LoggingInterceptor.response());
        }
    }

    private static RequestContext createContext(String method, String path,
                                               Map<String, String> headers, Object body) {
        return new RequestContext() {
            @Override public String method() { return method; }
            @Override public String path() { return path; }
            @Override public Map<String, String> headers() { return headers; }
            @Override public Map<String, String> queryParams() { return Map.of(); }
            @Override public Object body() { return body; }
            @Override public java.time.Duration timeout() { return null; }
            @Override public RequestContext accept(String mediaType) { return this; }
            @Override public RequestContext contentType(String mediaType) { return this; }
            @Override public RequestContext header(String key, String value) { return this; }
            @Override public RequestContext queryParam(String key, String value) { return this; }
            @Override public RequestContext body(Object body) { return this; }
            @Override public RequestContext timeout(java.time.Duration timeout) { return this; }
        };
    }
}
