package xyz.juandiii.ark.core.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.Ark;
import xyz.juandiii.ark.core.ArkClient;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.exceptions.ApiException;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code retrieveStream()} behavior at the client level with mocked transports.
 */
@ExtendWith(MockitoExtension.class)
class StreamingRequestTest {

    interface Both extends HttpTransport, StreamingTransport {}

    @Mock JsonSerializer serializer;
    @Mock HttpTransport transport;
    @Mock StreamingTransport streaming;

    private Ark client() {
        return ArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .streamingTransport(streaming)
                .baseUrl("https://api.example.com")
                .build();
    }

    private static StreamingResponse response(int status, Map<String, List<String>> headers, InputStream in) {
        return new DefaultStreamingResponse(status, headers, in, null);
    }

    @Test
    void retrieveStream_200_returnsTransportStream() throws Exception {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
        when(streaming.sendStreaming(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(response(200, Map.of("etag", List.of("\"v1\"")), new ByteArrayInputStream(bytes)));

        try (StreamingResponse r = client().get("/f").retrieveStream()) {
            assertEquals(200, r.statusCode());
            assertEquals("\"v1\"", r.header("ETag").orElseThrow());
            assertArrayEquals(bytes, r.body().readAllBytes());
        }
    }

    @Test
    void retrieveStream_304_doesNotThrow() {
        when(streaming.sendStreaming(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(response(304, Map.of(), null));

        try (StreamingResponse r = client().get("/f").retrieveStream()) {
            assertEquals(304, r.statusCode());
        }
    }

    @Test
    void retrieveStream_404_throwOnError_throwsApiExceptionWithBody() {
        AtomicBoolean closed = new AtomicBoolean();
        InputStream in = new FilterInputStream(new ByteArrayInputStream("not found".getBytes(StandardCharsets.UTF_8))) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
        when(streaming.sendStreaming(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(response(404, Map.of(), in));

        ApiException ex = assertThrows(ApiException.class, () -> client().get("/f").retrieveStream());

        assertEquals(404, ex.statusCode());
        assertTrue(ex.responseBody().contains("not found"));
        assertTrue(closed.get());
    }

    @Test
    void retrieveStream_404_noThrow_returnsStream() throws Exception {
        when(streaming.sendStreaming(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(response(404, Map.of(), new ByteArrayInputStream("nope".getBytes(StandardCharsets.UTF_8))));

        try (StreamingResponse r = client().get("/f").noThrow().retrieveStream()) {
            assertEquals(404, r.statusCode());
            assertEquals("nope", new String(r.body().readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void retrieveStream_withoutStreamingTransport_throwsUnsupported() {
        Ark plain = ArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .build();

        assertThrows(UnsupportedOperationException.class, () -> plain.get("/f").retrieveStream());
    }

    @Test
    void builder_autoDetectsStreamingTransport() {
        Both both = mock(Both.class);
        when(both.sendStreaming(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(response(200, Map.of(), null));
        Ark auto = ArkClient.builder()
                .serializer(serializer)
                .transport(both)
                .baseUrl("https://api.example.com")
                .build();

        try (StreamingResponse r = auto.get("/f").retrieveStream()) {
            assertEquals(200, r.statusCode());
        }

        verify(both).sendStreaming(eq("GET"), any(URI.class), anyMap(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void retrieveStream_appliesRequestInterceptors() {
        when(streaming.sendStreaming(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(response(200, Map.of(), null));
        Ark withInterceptor = ArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .streamingTransport(streaming)
                .baseUrl("https://api.example.com")
                .requestInterceptor(req -> req.header("X-Added", "yes"))
                .build();

        try (StreamingResponse r = withInterceptor.get("/f").retrieveStream()) {
            assertEquals(200, r.statusCode());
        }

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(streaming).sendStreaming(anyString(), any(), captor.capture(), any(), any());
        assertEquals("yes", captor.getValue().get("X-Added"));
    }

    @Test
    void defaultStreamingResponse_closeIsIdempotent() {
        AtomicInteger count = new AtomicInteger();
        DefaultStreamingResponse r = new DefaultStreamingResponse(200, Map.of(), null, count::incrementAndGet);

        r.close();
        r.close();

        assertEquals(1, count.get());
    }
}
