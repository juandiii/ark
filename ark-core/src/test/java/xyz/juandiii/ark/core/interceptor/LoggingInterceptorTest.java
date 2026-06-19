package xyz.juandiii.ark.core.interceptor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.juandiii.ark.core.AbstractArkBuilder;
import xyz.juandiii.ark.core.http.RawResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class LoggingInterceptorTest {

    static class TestBuilder extends AbstractArkBuilder<TestBuilder> {
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
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.NONE);

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
        void givenApplyWithQueryParams_thenRequestIncludesParams() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.BASIC);

            RequestContext context = createContextWithParams("GET", "/users",
                    Map.of(), null, Map.of("page", "1", "size", "10"));
            assertDoesNotThrow(() -> builder.reqList.get(0).intercept(context));
        }

        @Test
        void givenRequestWithNullHeaders_thenDoesNotThrow() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.HEADERS);

            RequestContext context = createContext("GET", "/users", null, null);
            assertDoesNotThrow(() -> builder.reqList.get(0).intercept(context));
        }

        @Test
        void givenRequestWithEmptyHeaders_thenDoesNotThrow() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.HEADERS);

            RequestContext context = createContext("GET", "/users", Map.of(), null);
            assertDoesNotThrow(() -> builder.reqList.get(0).intercept(context));
        }

        @Test
        void givenRequestWithNullBody_thenDoesNotThrow() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.BODY);

            RequestContext context = createContext("GET", "/users", Map.of(), null);
            assertDoesNotThrow(() -> builder.reqList.get(0).intercept(context));
        }

        @Test
        void givenApplyWithNullQueryParams_thenDoesNotThrow() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.BASIC);

            RequestContext context = createContextWithParams("GET", "/users",
                    Map.of(), null, null);
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
        void givenNullHeaders_thenDoesNotThrow() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.HEADERS);

            builder.reqList.get(0).intercept(createContext("GET", "/", Map.of(), null));
            RawResponse raw = new RawResponse(200, null, "ok");

            assertDoesNotThrow(() -> builder.resList.get(0).intercept(raw));
        }

        @Test
        void givenEmptyHeaders_thenDoesNotThrow() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.HEADERS);

            builder.reqList.get(0).intercept(createContext("GET", "/", Map.of(), null));
            RawResponse raw = new RawResponse(200, Map.of(), "ok");

            assertDoesNotThrow(() -> builder.resList.get(0).intercept(raw));
        }

        @Test
        void givenNullBodyWithBodyLevel_thenDoesNotThrow() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.BODY);

            builder.reqList.get(0).intercept(createContext("GET", "/", Map.of(), null));
            RawResponse raw = new RawResponse(200, Map.of(), null);

            assertDoesNotThrow(() -> builder.resList.get(0).intercept(raw));
        }

        @Test
        void givenResponseWithoutRequestStart_thenDurationIsNegative() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.BASIC);

            // Call response interceptor WITHOUT calling request interceptor first
            // ThreadLocal will be null, duration should be -1
            RawResponse raw = new RawResponse(200, Map.of(), "ok");
            RawResponse result = builder.resList.get(0).intercept(raw);

            assertSame(raw, result);
        }

        @Test
        void givenNullHeadersAndNullBody_thenDoesNotThrow() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.BODY);

            builder.reqList.get(0).intercept(createContext("GET", "/", Map.of(), null));
            RawResponse raw = new RawResponse(204, null, null);

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
    class HeaderRedaction {

        private Logger jul;
        private Level previousLevel;
        private CapturingHandler handler;

        @BeforeEach
        void attachHandler() {
            jul = Logger.getLogger("xyz.juandiii.ark");
            previousLevel = jul.getLevel();
            jul.setLevel(Level.ALL);
            handler = new CapturingHandler();
            handler.setLevel(Level.ALL);
            jul.addHandler(handler);
        }

        @AfterEach
        void detachHandler() {
            jul.removeHandler(handler);
            jul.setLevel(previousLevel);
        }

        @Test
        void givenAuthorizationHeader_whenLevelHeaders_thenValueIsRedacted() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.HEADERS);

            Map<String, String> headers = orderedHeaders("Authorization", "Bearer secret123");
            builder.reqList.get(0).intercept(createContext("GET", "/users", headers, null));

            String logged = handler.lastMessage();
            assertTrue(logged.contains("Authorization: [REDACTED]"), logged);
            assertFalse(logged.contains("Bearer secret123"), logged);
        }

        @Test
        void givenCookieHeader_whenLevelHeaders_thenValueIsRedacted() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.HEADERS);

            Map<String, String> headers = orderedHeaders("Cookie", "session=abc");
            builder.reqList.get(0).intercept(createContext("GET", "/users", headers, null));

            String logged = handler.lastMessage();
            assertTrue(logged.contains("Cookie: [REDACTED]"), logged);
            assertFalse(logged.contains("session=abc"), logged);
        }

        @Test
        void givenXApiKeyHeader_caseInsensitive_whenLevelHeaders_thenValueIsRedacted() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.HEADERS);

            Map<String, String> headers = orderedHeaders("x-Api-Key", "k-12345");
            builder.reqList.get(0).intercept(createContext("GET", "/users", headers, null));

            String logged = handler.lastMessage();
            assertTrue(logged.contains("x-Api-Key: [REDACTED]"), logged);
            assertFalse(logged.contains("k-12345"), logged);
        }

        @Test
        void givenNonSensitiveHeader_whenLevelHeaders_thenValueIsPreserved() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.HEADERS);

            Map<String, String> headers = orderedHeaders("Content-Type", "application/json");
            builder.reqList.get(0).intercept(createContext("POST", "/users", headers, null));

            String logged = handler.lastMessage();
            assertTrue(logged.contains("Content-Type: application/json"), logged);
            assertFalse(logged.contains("[REDACTED]"), logged);
        }

        @Test
        void givenLevelBasic_thenHeadersAreNotLogged() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.BASIC);

            Map<String, String> headers = orderedHeaders("Authorization", "Bearer secret123");
            builder.reqList.get(0).intercept(createContext("GET", "/users", headers, null));

            String logged = handler.lastMessage();
            assertFalse(logged.contains("Authorization"), logged);
            assertFalse(logged.contains("Bearer secret123"), logged);
        }

        @Test
        void givenLevelNone_thenNothingIsLogged() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.NONE);

            assertTrue(builder.reqList.isEmpty());
            assertTrue(handler.records.isEmpty());
        }

        @Test
        void givenResponseAuthorizationHeader_thenValueIsRedacted() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.HEADERS);

            builder.reqList.get(0).intercept(createContext("GET", "/", Map.of(), null));
            handler.records.clear();

            RawResponse raw = new RawResponse(200,
                    Map.of("Set-Cookie", List.of("session=xyz", "csrf=abc")),
                    "ok");
            builder.resList.get(0).intercept(raw);

            String logged = handler.lastMessage();
            assertTrue(logged.contains("Set-Cookie: [REDACTED], [REDACTED]"), logged);
            assertFalse(logged.contains("session=xyz"), logged);
            assertFalse(logged.contains("csrf=abc"), logged);
        }

        @Test
        void givenRequestContextHeaders_afterLogging_thenOriginalMapUnchanged() {
            var builder = new TestBuilder();
            LoggingInterceptor.apply(builder, LoggingInterceptor.Level.HEADERS);

            Map<String, String> headers = orderedHeaders("Authorization", "Bearer keep-me");
            RequestContext context = createContext("GET", "/", headers, null);
            builder.reqList.get(0).intercept(context);

            assertEquals("Bearer keep-me", context.headers().get("Authorization"));
        }

        private static Map<String, String> orderedHeaders(String... kv) {
            Map<String, String> m = new LinkedHashMap<>();
            for (int i = 0; i < kv.length; i += 2) {
                m.put(kv[i], kv[i + 1]);
            }
            return m;
        }

        private static final class CapturingHandler extends Handler {
            final List<LogRecord> records = new ArrayList<>();

            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override public void flush() {}
            @Override public void close() {}

            String lastMessage() {
                if (records.isEmpty()) return "";
                return records.get(records.size() - 1).getMessage();
            }
        }
    }

    @Nested
    class ParseLevel {

        @Test
        void givenNull_thenReturnsOff() {
            assertEquals(LoggingInterceptor.Level.NONE, LoggingInterceptor.parseLevel(null));
        }

        @Test
        void givenValid_thenReturnsLevel() {
            assertEquals(LoggingInterceptor.Level.BODY, LoggingInterceptor.parseLevel("BODY"));
            assertEquals(LoggingInterceptor.Level.BODY, LoggingInterceptor.parseLevel("body"));
        }

        @Test
        void givenInvalid_thenReturnsOff() {
            assertEquals(LoggingInterceptor.Level.NONE, LoggingInterceptor.parseLevel("invalid"));
        }
    }

    private static RequestContext createContext(String method, String path,
                                               Map<String, String> headers, Object body) {
        return createContextWithParams(method, path, headers, body, Map.of());
    }

    private static RequestContext createContextWithParams(String method, String path,
                                                          Map<String, String> headers, Object body,
                                                          Map<String, String> queryParams) {
        return new RequestContext() {
            @Override public String method() { return method; }
            @Override public String path() { return path; }
            @Override public Map<String, String> headers() { return headers; }
            @Override public Map<String, String> queryParams() { return queryParams; }
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
