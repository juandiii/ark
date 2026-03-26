package xyz.juandiii.ark.async.proxy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.async.http.AsyncClientRequest;
import xyz.juandiii.ark.async.http.AsyncClientResponse;
import xyz.juandiii.ark.http.ArkResponse;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncReturnTypeHandlerTest {

    @Mock AsyncClientRequest request;
    @Mock AsyncClientResponse response;

    private final AsyncReturnTypeHandler handler = new AsyncReturnTypeHandler();

    // --- Helper to get generic types from test interfaces ---

    interface TypeHelper {
        CompletableFuture<String> futureString();
        CompletableFuture<ArkResponse<String>> futureArkResponse();
        CompletableFuture<ArkResponse<Void>> futureArkResponseVoid();
        CompletableFuture<Void> futureVoid();
        void voidReturn();
    }

    private Type returnTypeOf(String methodName) throws NoSuchMethodException {
        return TypeHelper.class.getMethod(methodName).getGenericReturnType();
    }

    @Nested
    class FutureBody {

        @Test
        void givenCompletableFutureString_thenCallsBody() throws Exception {
            CompletableFuture<String> expected = CompletableFuture.completedFuture("result");
            when(request.retrieve()).thenReturn(response);
            when(response.body(any(TypeRef.class))).thenReturn(expected);

            Object result = handler.handle(request, returnTypeOf("futureString"));

            assertSame(expected, result);
            verify(response).body(any(TypeRef.class));
        }
    }

    @Nested
    class FutureArkResponse {

        @Test
        void givenCompletableFutureArkResponse_thenCallsToEntity() throws Exception {
            CompletableFuture<ArkResponse<String>> expected =
                    CompletableFuture.completedFuture(new ArkResponse<>(200, Map.of(), "ok"));
            when(request.retrieve()).thenReturn(response);
            when(response.toEntity(any(TypeRef.class))).thenReturn(expected);

            Object result = handler.handle(request, returnTypeOf("futureArkResponse"));

            assertSame(expected, result);
            verify(response).toEntity(any(TypeRef.class));
        }

        @Test
        void givenCompletableFutureArkResponseVoid_thenCallsToBodilessEntity() throws Exception {
            CompletableFuture<ArkResponse<Void>> expected =
                    CompletableFuture.completedFuture(new ArkResponse<>(204, Map.of(), null));
            when(request.retrieve()).thenReturn(response);
            when(response.toBodilessEntity()).thenReturn(expected);

            Object result = handler.handle(request, returnTypeOf("futureArkResponseVoid"));

            assertSame(expected, result);
            verify(response).toBodilessEntity();
        }
    }

    @Nested
    class FutureVoid {

        @Test
        void givenCompletableFutureVoid_thenCallsToBodilessEntity() throws Exception {
            CompletableFuture<ArkResponse<Void>> expected =
                    CompletableFuture.completedFuture(new ArkResponse<>(204, Map.of(), null));
            when(request.retrieve()).thenReturn(response);
            when(response.toBodilessEntity()).thenReturn(expected);

            Object result = handler.handle(request, returnTypeOf("futureVoid"));

            assertSame(expected, result);
        }
    }

    @Nested
    class VoidReturn {

        @Test
        void givenVoid_thenCallsToBodilessEntity() throws Exception {
            CompletableFuture<ArkResponse<Void>> expected =
                    CompletableFuture.completedFuture(new ArkResponse<>(204, Map.of(), null));
            when(request.retrieve()).thenReturn(response);
            when(response.toBodilessEntity()).thenReturn(expected);

            Object result = handler.handle(request, returnTypeOf("voidReturn"));

            assertSame(expected, result);
        }
    }
}
