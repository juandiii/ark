package xyz.juandiii.ark.reactor.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.reactor.ReactorArk;
import xyz.juandiii.ark.reactor.ReactorArkClient;
import xyz.juandiii.ark.reactor.proxy.ReactorReturnTypeHandler;

import java.lang.reflect.Type;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies fluent {@code .raw()} access and proxy {@code RawResponse} return type
 * auto-toggling {@code noThrow} for the Reactor execution model.
 */
@ExtendWith(MockitoExtension.class)
class ReactorRawResponseAccessTest {

    @Mock JsonSerializer serializer;
    @Mock ReactorHttpTransport transport;

    interface TypeHelper {
        Mono<RawResponse> monoRaw();
    }

    private ReactorArk defaultClient() {
        return ReactorArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .build();
    }

    @Test
    void fluentRaw_200_returnsTransportRawResponse() {
        RawResponse expected = new RawResponse(200, Map.of(), "ok");
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Mono.just(expected));

        StepVerifier.create(defaultClient().get("/foo").retrieve().raw())
                .assertNext(raw -> {
                    assertSame(expected, raw);
                    assertEquals(200, raw.statusCode());
                })
                .verifyComplete();
    }

    @Test
    void fluentRawWithNoThrow_404_doesNotThrowAndExposesStatusAndBody() {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Mono.just(new RawResponse(404, Map.of(), "{\"error\":\"missing\"}")));

        StepVerifier.create(defaultClient().get("/foo").noThrow().retrieve().raw())
                .assertNext(raw -> {
                    assertEquals(404, raw.statusCode());
                    assertEquals("{\"error\":\"missing\"}", raw.body());
                    assertTrue(raw.isError());
                })
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void proxyMonoRawResponseReturnType_404_autoNoThrowAndReturnsRaw() throws Exception {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Mono.just(new RawResponse(404, Map.of(), "{\"err\":\"x\"}")));

        ReactorArk ark = defaultClient();
        ReactorReturnTypeHandler handler = new ReactorReturnTypeHandler();
        Type returnType = TypeHelper.class.getMethod("monoRaw").getGenericReturnType();

        Object result = handler.handle(ark.get("/foo"), returnType);

        assertInstanceOf(Mono.class, result);
        StepVerifier.create((Mono<RawResponse>) result)
                .assertNext(raw -> {
                    assertEquals(404, raw.statusCode());
                    assertEquals("{\"err\":\"x\"}", raw.body());
                })
                .verifyComplete();
    }
}
