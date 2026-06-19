package xyz.juandiii.ark.core.http;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.exceptions.ApiException;
import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.core.interceptor.RequestInterceptor;
import xyz.juandiii.ark.core.interceptor.ResponseInterceptor;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractClientRequestTest {

    @Mock
    HttpTransport transport;

    @Mock
    JsonSerializer serializer;

    private DefaultClientRequest request(String method, String path) {
        return request(method, "https://api.example.com", path);
    }

    private DefaultClientRequest request(String method, String baseUrl, String path) {
        return new DefaultClientRequest(method, baseUrl, path,
                transport, serializer, Collections.emptyList(), Collections.emptyList());
    }

    @Nested
    class UriBuilding {

        @Test
        void buildsSimpleUri() {
            assertEquals(URI.create("https://api.example.com/users"),
                    request("GET", "/users").buildUri());
        }

        @Test
        void buildsUriWithQueryParams() {
            DefaultClientRequest req = request("GET", "/users");
            req.queryParam("page", "1").queryParam("size", "20");
            assertEquals(URI.create("https://api.example.com/users?page=1&size=20"),
                    req.buildUri());
        }

        @Test
        void encodesQueryParamValues() {
            DefaultClientRequest req = request("GET", "/search");
            req.queryParam("q", "hello world");
            assertTrue(req.buildUri().toString().contains("q=hello+world"));
        }

        @Test
        void ignoresNullQueryParamValue() {
            DefaultClientRequest req = request("GET", "/users");
            req.queryParam("page", null);
            assertFalse(req.buildUri().toString().contains("?"));
        }

        @Test
        void givenTrailingSlashOnBaseUrl_whenPathStartsWithSlash_thenNormalizesDoubleSlash() {
            assertEquals(URI.create("https://api.example.com/users"),
                    request("GET", "https://api.example.com/", "/users").buildUri());
        }

        @Test
        void givenTrailingSlashOnBaseUrl_whenPathIsSlash_thenNoTrailingSlash() {
            assertEquals(URI.create("https://api.example.com"),
                    request("GET", "https://api.example.com/", "/").buildUri());
        }

        @Test
        void givenTrailingSlashOnBaseUrl_whenPathIsEmpty_thenNoTrailingSlash() {
            assertEquals(URI.create("https://api.example.com"),
                    request("GET", "https://api.example.com/", "").buildUri());
        }

        @Test
        void givenMultipleSlashes_whenBuildUri_thenNormalizesToSingle() {
            assertEquals(URI.create("https://api.example.com/users"),
                    request("GET", "https://api.example.com///", "///users").buildUri());
        }

        @Test
        void givenNoTrailingSlash_whenPathHasNoLeadingSlash_thenConcatenatesCorrectly() {
            assertEquals(URI.create("https://api.example.com/users"),
                    request("GET", "https://api.example.com", "/users").buildUri());
        }

        @Test
        void givenHttpsProtocol_whenNormalizing_thenPreservesProtocolSlashes() {
            assertEquals(URI.create("https://api.example.com/users"),
                    request("GET", "https://api.example.com", "/users").buildUri());
        }

        @Test
        void invalidUriThrowsArkException() {
            DefaultClientRequest req = request("GET", "/users/{id}");
            assertThrows(ArkException.class, req::buildUri);
        }
    }

    @Nested
    class Headers {

        @Test
        void rejectsNullKey() {
            assertThrows(NullPointerException.class,
                    () -> request("GET", "/").header(null, "value"));
        }

        @Test
        void ignoresNullValue() {
            DefaultClientRequest req = request("GET", "/");
            req.header("X-Custom", null);
            assertFalse(req.headers().containsKey("X-Custom"));
        }

        @Test
        void setsAcceptHeader() {
            DefaultClientRequest req = request("GET", "/");
            req.accept("application/json");
            assertEquals("application/json", req.headers().get("Accept"));
        }

        @Test
        void setsContentTypeHeader() {
            DefaultClientRequest req = request("POST", "/");
            req.contentType("application/json");
            assertEquals("application/json", req.headers().get("Content-Type"));
        }
    }

    @Nested
    class FluentChaining {

        @Test
        void allMethodsReturnSameInstance() {
            DefaultClientRequest req = request("GET", "/");
            assertSame(req, req.accept("application/json"));
            assertSame(req, req.contentType("text/plain"));
            assertSame(req, req.header("X-Key", "value"));
            assertSame(req, req.queryParam("k", "v"));
            assertSame(req, req.body("test"));
            assertSame(req, req.timeout(Duration.ofSeconds(5)));
        }
    }

    @Nested
    class BodySerialization {

        @Test
        void needsBodyForPostPutPatch() {
            assertTrue(request("POST", "/").needsBody());
            assertTrue(request("PUT", "/").needsBody());
            assertTrue(request("PATCH", "/").needsBody());
        }

        @Test
        void doesNotNeedBodyForGetDelete() {
            assertFalse(request("GET", "/").needsBody());
            assertFalse(request("DELETE", "/").needsBody());
        }

        @Test
        void serializesBodyForPost() {
            when(serializer.serialize("payload")).thenReturn("{\"data\":\"payload\"}");
            DefaultClientRequest req = request("POST", "/");
            req.body("payload");
            assertEquals("{\"data\":\"payload\"}", req.serializeBody());
            verify(serializer).serialize("payload");
        }

        @Test
        void returnsNullBodyForGet() {
            DefaultClientRequest req = request("GET", "/");
            req.body("ignored");
            assertNull(req.serializeBody());
            verifyNoInteractions(serializer);
        }
    }

    @Nested
    class ResponseValidation {

        @Test
        void passesOnSuccessStatus() {
            RawResponse ok = new RawResponse(200, Map.of(), "OK");
            assertDoesNotThrow(() -> request("GET", "/").validateResponse(ok));
        }

        @Test
        void throwsApiExceptionOnErrorStatus() {
            RawResponse error = new RawResponse(404, Map.of(), "Not Found");
            ApiException ex = assertThrows(ApiException.class,
                    () -> request("GET", "/").validateResponse(error));
            assertEquals(404, ex.statusCode());
            assertEquals("Not Found", ex.responseBody());
        }
    }

    @Nested
    class Retrieve {

        @Test
        void callsTransportWithCorrectArgs() {
            when(transport.send(anyString(), any(URI.class), anyMap(), isNull(), isNull()))
                    .thenReturn(new RawResponse(200, Map.of(), "{}"));

            DefaultClientRequest req = request("GET", "/users");
            req.accept("application/json");
            req.retrieve();

            ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
            ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);

            verify(transport).send(eq("GET"), uriCaptor.capture(), headersCaptor.capture(), isNull(), isNull());
            assertEquals(URI.create("https://api.example.com/users"), uriCaptor.getValue());
            assertEquals("application/json", headersCaptor.getValue().get("Accept"));
        }

        @Test
        void throwsOnErrorResponse() {
            when(transport.send(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(new RawResponse(500, Map.of(), "Server Error"));

            assertThrows(ApiException.class, () -> request("GET", "/fail").retrieve());
        }

        @Test
        void passesTimeoutToTransport() {
            when(transport.send(anyString(), any(), anyMap(), any(), eq(Duration.ofSeconds(30))))
                    .thenReturn(new RawResponse(200, Map.of(), "{}"));

            request("GET", "/").timeout(Duration.ofSeconds(30)).retrieve();

            verify(transport).send(anyString(), any(), anyMap(), any(), eq(Duration.ofSeconds(30)));
        }
    }

    @Nested
    class Interceptors {

        @Test
        void requestInterceptorRunsBeforeSend() {
            RequestInterceptor interceptor = mock(RequestInterceptor.class);
            when(transport.send(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(new RawResponse(200, Map.of(), "{}"));

            DefaultClientRequest req = new DefaultClientRequest("GET", "https://api.example.com", "/",
                    transport, serializer, List.of(interceptor), Collections.emptyList());
            req.retrieve();

            verify(interceptor).intercept(req);
        }

        @Test
        void responseInterceptorTransformsResponse() {
            ResponseInterceptor interceptor = raw ->
                    new RawResponse(raw.statusCode(), raw.headers(), "modified");
            when(transport.send(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(new RawResponse(200, Map.of(), "original"));

            DefaultClientRequest req = new DefaultClientRequest("GET", "https://api.example.com", "/",
                    transport, serializer, Collections.emptyList(), List.of(interceptor));

            ClientResponse response = req.retrieve();
            assertNotNull(response);
        }

        @Test
        void multipleInterceptorsRunInOrder() {
            StringBuilder order = new StringBuilder();
            RequestInterceptor first = req -> order.append("1");
            RequestInterceptor second = req -> order.append("2");
            when(transport.send(anyString(), any(), anyMap(), any(), any()))
                    .thenReturn(new RawResponse(200, Map.of(), "{}"));

            DefaultClientRequest req = new DefaultClientRequest("GET", "https://api.example.com", "/",
                    transport, serializer, List.of(first, second), Collections.emptyList());
            req.retrieve();

            assertEquals("12", order.toString());
        }
    }
}