package xyz.juandiii.ark.interceptor;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.juandiii.ark.http.RawResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoggingInterceptorTest {

    static class TestBuilder extends xyz.juandiii.ark.AbstractArkBuilder<TestBuilder> {
        final List<RequestInterceptor> reqList = new ArrayList<>();
        final List<ResponseInterceptor> resList = new ArrayList<>();

        @Override
        public TestBuilder requestInterceptor(RequestInterceptor interceptor) {
            reqList.add(interceptor);
            return super.requestInterceptor(interceptor);
        }

        @Override
        public TestBuilder responseInterceptor(ResponseInterceptor interceptor) {
            resList.add(interceptor);
            return super.responseInterceptor(interceptor);
        }
    }

    @Nested
    class Apply {

        @Test
        void givenOffLevel_thenNoInterceptorsAdded() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.OFF);

            assertTrue(builder.reqList.isEmpty());
            assertTrue(builder.resList.isEmpty());
        }

        @Test
        void givenBasicLevel_thenAddsInterceptors() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.BASIC);

            assertEquals(1, builder.reqList.size());
            assertEquals(1, builder.resList.size());
        }

        @Test
        void givenHeadersLevel_thenAddsInterceptors() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.HEADERS);

            assertEquals(1, builder.reqList.size());
            assertEquals(1, builder.resList.size());
        }

        @Test
        void givenBodyLevel_thenAddsInterceptors() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.BODY);

            assertEquals(1, builder.reqList.size());
            assertEquals(1, builder.resList.size());
        }

        @Test
        void givenApply_thenRequestInterceptorExecutes() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.BODY);

            RequestContext context = createContext("GET", "/users", Map.of("Accept", "application/json"), null);
            assertDoesNotThrow(() -> builder.reqList.get(0).intercept(context));
        }

        @Test
        void givenApply_thenResponseInterceptorReturnsRaw() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.BODY);

            builder.reqList.get(0).intercept(createContext("GET", "/", Map.of(), null));
            RawResponse raw = new RawResponse(200, Map.of("Content-Type", List.of("application/json")), "{\"ok\":true}");
            RawResponse result = builder.resList.get(0).intercept(raw);

            assertSame(raw, result);
        }

        @Test
        void givenErrorResponse_thenDoesNotThrow() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.BASIC);

            builder.reqList.get(0).intercept(createContext("GET", "/", Map.of(), null));
            RawResponse raw = new RawResponse(500, Map.of(), "error");

            assertDoesNotThrow(() -> builder.resList.get(0).intercept(raw));
        }

        @Test
        void givenLargeBody_thenDoesNotThrow() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.BODY);

            builder.reqList.get(0).intercept(createContext("GET", "/", Map.of(), null));
            RawResponse raw = new RawResponse(200, Map.of(), "x".repeat(2000));

            assertDoesNotThrow(() -> builder.resList.get(0).intercept(raw));
        }

        @Test
        void givenNullBody_thenDoesNotThrow() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.BODY);

            builder.reqList.get(0).intercept(createContext("POST", "/users", Map.of(), "body"));
            RawResponse raw = new RawResponse(204, Map.of(), null);

            assertDoesNotThrow(() -> builder.resList.get(0).intercept(raw));
        }
    }

    @Nested
    class ParseLevel {

        @Test
        void givenNull_thenReturnsOff() {
            assertEquals(LoggingInterceptor.Level.OFF, LoggingInterceptor.parseLevel(null));
        }

        @Test
        void givenValid_thenReturnsLevel() {
            assertEquals(LoggingInterceptor.Level.BODY, LoggingInterceptor.parseLevel("BODY"));
            assertEquals(LoggingInterceptor.Level.BODY, LoggingInterceptor.parseLevel("body"));
        }

        @Test
        void givenInvalid_thenReturnsOff() {
            assertEquals(LoggingInterceptor.Level.OFF, LoggingInterceptor.parseLevel("invalid"));
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
