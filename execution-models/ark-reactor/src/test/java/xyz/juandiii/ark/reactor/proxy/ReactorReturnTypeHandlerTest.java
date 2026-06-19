package xyz.juandiii.ark.reactor.proxy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.juandiii.ark.core.TypeRef;
import xyz.juandiii.ark.core.http.ArkResponse;
import xyz.juandiii.ark.reactor.http.ReactorClientRequest;
import xyz.juandiii.ark.reactor.http.ReactorClientResponse;

import java.lang.reflect.Type;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactorReturnTypeHandlerTest {

    @Mock ReactorClientRequest request;
    @Mock ReactorClientResponse response;

    private final ReactorReturnTypeHandler handler = new ReactorReturnTypeHandler();

    interface TypeHelper {
        Mono<String> monoString();
        Mono<ArkResponse<String>> monoArkResponse();
        Mono<ArkResponse<Void>> monoArkResponseVoid();
        Mono<Void> monoVoid();
        Flux<String> fluxString();
        void voidReturn();
    }

    private Type returnTypeOf(String methodName) throws NoSuchMethodException {
        return TypeHelper.class.getMethod(methodName).getGenericReturnType();
    }

    @Nested
    class MonoBody {

        @Test
        void givenMonoString_thenCallsBody() throws Exception {
            Mono<String> expected = Mono.just("result");
            when(request.retrieve()).thenReturn(response);
            when(response.body(any(TypeRef.class))).thenReturn(expected);

            Object result = handler.handle(request, returnTypeOf("monoString"));

            assertSame(expected, result);
            verify(response).body(any(TypeRef.class));
        }
    }

    @Nested
    class MonoArkResponse {

        @Test
        void givenMonoArkResponse_thenCallsToEntity() throws Exception {
            Mono<ArkResponse<String>> expected =
                    Mono.just(new ArkResponse<>(200, Map.of(), "ok"));
            when(request.retrieve()).thenReturn(response);
            when(response.toEntity(any(TypeRef.class))).thenReturn(expected);

            Object result = handler.handle(request, returnTypeOf("monoArkResponse"));

            assertSame(expected, result);
            verify(response).toEntity(any(TypeRef.class));
        }

        @Test
        void givenMonoArkResponseVoid_thenCallsToBodilessEntity() throws Exception {
            Mono<ArkResponse<Void>> expected =
                    Mono.just(new ArkResponse<>(204, Map.of(), null));
            when(request.retrieve()).thenReturn(response);
            when(response.toBodilessEntity()).thenReturn(expected);

            Object result = handler.handle(request, returnTypeOf("monoArkResponseVoid"));

            assertSame(expected, result);
            verify(response).toBodilessEntity();
        }
    }

    @Nested
    class MonoVoid {

        @Test
        void givenMonoVoid_thenCallsToBodilessEntity() throws Exception {
            Mono<ArkResponse<Void>> expected =
                    Mono.just(new ArkResponse<>(204, Map.of(), null));
            when(request.retrieve()).thenReturn(response);
            when(response.toBodilessEntity()).thenReturn(expected);

            Object result = handler.handle(request, returnTypeOf("monoVoid"));

            assertSame(expected, result);
        }
    }

    @Nested
    class FluxBody {

        @Test
        void givenFluxString_thenDeserializesListAndStreams() throws Exception {
            Flux<String> expected = Flux.just("Juan", "Pedro", "Maria");
            when(request.retrieve()).thenReturn(response);
            when(response.bodyAsFlux(any(TypeRef.class))).thenReturn(expected);

            Object result = handler.handle(request, returnTypeOf("fluxString"));

            assertSame(expected, result);
            verify(response).bodyAsFlux(any(TypeRef.class));
        }
    }

    @Nested
    class VoidReturn {

        @Test
        void givenVoid_thenCallsToBodilessEntity() throws Exception {
            Mono<ArkResponse<Void>> expected =
                    Mono.just(new ArkResponse<>(204, Map.of(), null));
            when(request.retrieve()).thenReturn(response);
            when(response.toBodilessEntity()).thenReturn(expected);

            Object result = handler.handle(request, returnTypeOf("voidReturn"));

            assertSame(expected, result);
        }
    }
}