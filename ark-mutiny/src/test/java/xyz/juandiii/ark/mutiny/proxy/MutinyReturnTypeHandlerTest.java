package xyz.juandiii.ark.mutiny.proxy;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.http.ArkResponse;
import xyz.juandiii.ark.mutiny.http.MutinyClientRequest;
import xyz.juandiii.ark.mutiny.http.MutinyClientResponse;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MutinyReturnTypeHandlerTest {

    @Mock MutinyClientRequest request;
    @Mock MutinyClientResponse response;

    private final MutinyReturnTypeHandler handler = new MutinyReturnTypeHandler();

    interface TypeHelper {
        Uni<String> uniString();
        Uni<ArkResponse<String>> uniArkResponse();
        Uni<ArkResponse<Void>> uniArkResponseVoid();
        Uni<Void> uniVoid();
        Multi<String> multiString();
        void voidReturn();
    }

    private Type returnTypeOf(String methodName) throws NoSuchMethodException {
        return TypeHelper.class.getMethod(methodName).getGenericReturnType();
    }

    @Nested
    class UniBody {

        @Test
        void givenUniString_thenCallsBody() throws Exception {
            Uni<String> expected = Uni.createFrom().item("result");
            when(request.retrieve()).thenReturn(response);
            when(response.body(any(TypeRef.class))).thenReturn(expected);

            Object result = handler.handle(request, returnTypeOf("uniString"));

            assertSame(expected, result);
            verify(response).body(any(TypeRef.class));
        }
    }

    @Nested
    class UniArkResponse {

        @Test
        void givenUniArkResponse_thenCallsToEntity() throws Exception {
            Uni<ArkResponse<String>> expected =
                    Uni.createFrom().item(new ArkResponse<>(200, Map.of(), "ok"));
            when(request.retrieve()).thenReturn(response);
            when(response.toEntity(any(TypeRef.class))).thenReturn(expected);

            Object result = handler.handle(request, returnTypeOf("uniArkResponse"));

            assertSame(expected, result);
            verify(response).toEntity(any(TypeRef.class));
        }

        @Test
        void givenUniArkResponseVoid_thenCallsToBodilessEntity() throws Exception {
            Uni<ArkResponse<Void>> expected =
                    Uni.createFrom().item(new ArkResponse<>(204, Map.of(), null));
            when(request.retrieve()).thenReturn(response);
            when(response.toBodilessEntity()).thenReturn(expected);

            Object result = handler.handle(request, returnTypeOf("uniArkResponseVoid"));

            assertSame(expected, result);
            verify(response).toBodilessEntity();
        }
    }

    @Nested
    class UniVoid {

        @Test
        void givenUniVoid_thenCallsToBodilessEntity() throws Exception {
            Uni<ArkResponse<Void>> expected =
                    Uni.createFrom().item(new ArkResponse<>(204, Map.of(), null));
            when(request.retrieve()).thenReturn(response);
            when(response.toBodilessEntity()).thenReturn(expected);

            Object result = handler.handle(request, returnTypeOf("uniVoid"));

            assertSame(expected, result);
        }
    }

    @Nested
    class MultiBody {

        @Test
        void givenMultiString_thenCallsBodyAsMulti() throws Exception {
            Multi<String> expected = Multi.createFrom().items("Juan", "Pedro", "Maria");
            when(request.retrieve()).thenReturn(response);
            when(response.bodyAsMulti(any(TypeRef.class))).thenReturn(expected);

            Object result = handler.handle(request, returnTypeOf("multiString"));

            assertSame(expected, result);
            verify(response).bodyAsMulti(any(TypeRef.class));
        }
    }

    @Nested
    class VoidReturn {

        @Test
        void givenVoid_thenCallsToBodilessEntity() throws Exception {
            Uni<ArkResponse<Void>> expected =
                    Uni.createFrom().item(new ArkResponse<>(204, Map.of(), null));
            when(request.retrieve()).thenReturn(response);
            when(response.toBodilessEntity()).thenReturn(expected);

            Object result = handler.handle(request, returnTypeOf("voidReturn"));

            assertSame(expected, result);
        }
    }
}
