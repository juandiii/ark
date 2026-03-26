package xyz.juandiii.ark.proxy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.Ark;
import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.exceptions.ArkException;
import xyz.juandiii.ark.http.ArkResponse;
import xyz.juandiii.ark.http.ClientRequest;
import xyz.juandiii.ark.http.ClientResponse;
import xyz.juandiii.ark.interceptor.RequestContext;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArkProxyTest {

    @Mock Ark ark;
    @Mock ClientRequest clientRequest;
    @Mock ClientResponse clientResponse;

    // --- Stub collaborators ---

    static class StubAnnotationResolver implements AnnotationResolver {

        @Override
        public String resolveBasePath(Class<?> declaringClass) {
            return "/base";
        }

        @Override
        public MethodInfo resolveMethod(Method method) {
            return switch (method.getName()) {
                case "getItem" -> new MethodInfo("GET", "/items/{id}", null, null);
                case "createItem" -> new MethodInfo("POST", "/items", "application/json", "application/json");
                case "updateItem" -> new MethodInfo("PUT", "/items/{id}", null, null);
                case "patchItem" -> new MethodInfo("PATCH", "/items/{id}", null, null);
                case "deleteItem" -> new MethodInfo("DELETE", "/items/{id}", null, null);
                case "getItemFull" -> new MethodInfo("GET", "/items/{id}", null, null);
                default -> throw new ArkException("No method: " + method.getName());
            };
        }

        @Override
        public String resolvePath(String path, Method method, Object[] args) {
            if (args != null && args.length > 0) {
                path = path.replace("{id}", String.valueOf(args[0]));
            }
            return path;
        }

        @Override
        public String resolveSubResourcePath(Method method, Object[] args) {
            return "/sub";
        }

        @Override
        public boolean isSubResource(Method method) {
            return method.getReturnType() == SubApi.class;
        }
    }

    static class StubParameterBinder implements ParameterBinder {
        @Override
        public void apply(RequestContext request, Method method, Object[] args) {
            if (args != null && args.length > 1 && args[1] != null) {
                request.body(args[1]);
            }
        }
    }

    // --- Test interfaces ---

    interface ItemApi {
        String getItem(Long id);
        void createItem(Long ignored, Object body);
        String updateItem(Long id, Object body);
        String patchItem(Long id, Object body);
        void deleteItem(Long id);
        ArkResponse<String> getItemFull(Long id);
        SubApi subResource();
    }

    interface SubApi {
        String getItem(Long id);
    }

    static class NotAnInterface {}

    // --- Helpers ---

    private <T> T createProxy(Class<T> iface) {
        return ArkProxy.create(iface,
                Dispatchers.sync(ark),
                new StubAnnotationResolver(),
                new StubParameterBinder(),
                new SyncReturnTypeHandler());
    }

    @Nested
    class Create {

        @Test
        void givenInterface_whenCreate_thenReturnsProxy() {
            ItemApi proxy = createProxy(ItemApi.class);
            assertNotNull(proxy);
        }

        @Test
        void givenNonInterface_whenCreate_thenThrowsArkException() {
            assertThrows(ArkException.class, () -> createProxy(NotAnInterface.class));
        }
    }

    @Nested
    class HttpMethodDispatch {

        @Test
        void givenGet_thenCallsArkGet() {
            when(ark.get("/base/items/1")).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.body(any(TypeRef.class))).thenReturn("found");

            assertEquals("found", createProxy(ItemApi.class).getItem(1L));
            verify(ark).get("/base/items/1");
        }

        @Test
        void givenPost_thenCallsArkPost() {
            when(ark.post("/base/items")).thenReturn(clientRequest);
            when(clientRequest.contentType("application/json")).thenReturn(clientRequest);
            when(clientRequest.accept("application/json")).thenReturn(clientRequest);
            when(clientRequest.body(any())).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.toBodilessEntity()).thenReturn(new ArkResponse<>(201, Map.of(), null));

            createProxy(ItemApi.class).createItem(null, "payload");
            verify(ark).post("/base/items");
        }

        @Test
        void givenPut_thenCallsArkPut() {
            when(ark.put("/base/items/1")).thenReturn(clientRequest);
            when(clientRequest.body(any())).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.body(any(TypeRef.class))).thenReturn("updated");

            assertEquals("updated", createProxy(ItemApi.class).updateItem(1L, "body"));
            verify(ark).put("/base/items/1");
        }

        @Test
        void givenPatch_thenCallsArkPatch() {
            when(ark.patch("/base/items/1")).thenReturn(clientRequest);
            when(clientRequest.body(any())).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.body(any(TypeRef.class))).thenReturn("patched");

            assertEquals("patched", createProxy(ItemApi.class).patchItem(1L, "body"));
            verify(ark).patch("/base/items/1");
        }

        @Test
        void givenDelete_thenCallsArkDelete() {
            when(ark.delete("/base/items/1")).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.toBodilessEntity()).thenReturn(new ArkResponse<>(204, Map.of(), null));

            createProxy(ItemApi.class).deleteItem(1L);
            verify(ark).delete("/base/items/1");
        }
    }

    @Nested
    class ContentNegotiation {

        @Test
        void givenConsumesAndProduces_thenSetsHeaders() {
            when(ark.post("/base/items")).thenReturn(clientRequest);
            when(clientRequest.contentType("application/json")).thenReturn(clientRequest);
            when(clientRequest.accept("application/json")).thenReturn(clientRequest);
            when(clientRequest.body(any())).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.toBodilessEntity()).thenReturn(new ArkResponse<>(201, Map.of(), null));

            createProxy(ItemApi.class).createItem(null, "body");

            verify(clientRequest).contentType("application/json");
            verify(clientRequest).accept("application/json");
        }
    }

    @Nested
    class ArkResponseReturnType {

        @Test
        void givenArkResponseReturn_thenCallsToEntity() {
            when(ark.get("/base/items/1")).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.toEntity(any(TypeRef.class))).thenReturn(new ArkResponse<>(200, Map.of(), "Juan"));

            ArkResponse<String> result = createProxy(ItemApi.class).getItemFull(1L);

            assertEquals(200, result.statusCode());
            assertEquals("Juan", result.body());
        }
    }

    @Nested
    class SubResource {

        @Test
        void givenSubResource_thenCreatesSubProxy() {
            SubApi sub = createProxy(ItemApi.class).subResource();
            assertNotNull(sub);
        }
    }

    @Nested
    class AutoDetect {

        @Test
        void givenNoAnnotationProvider_thenThrowsArkException() {
            assertThrows(ArkException.class, () -> ArkProxy.create(SubApi.class, ark));
        }

        @Test
        void givenUnknownClient_thenThrowsArkException() {
            assertThrows(ArkException.class, () -> ArkProxy.create(ItemApi.class, "not-a-client"));
        }

        @Test
        void givenNonInterface_thenThrowsArkException() {
            assertThrows(ArkException.class, () -> ArkProxy.create(NotAnInterface.class, ark));
        }

        @Test
        void givenSyncArk_thenResolvesDispatcherAndHandler() {
            when(ark.get("/base/items/1")).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.body(any(TypeRef.class))).thenReturn("ok");

            ItemApi proxy = createProxy(ItemApi.class);
            assertEquals("ok", proxy.getItem(1L));
        }

        @Test
        void givenSyncArkAsObject_thenResolvesSyncModel() {
            when(ark.get("/base/items/1")).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.body(any(TypeRef.class))).thenReturn("ok");

            // Tests SyncReturnTypeHandler and Dispatchers.sync() path
            ItemApi proxy = createProxy(ItemApi.class);
            assertEquals("ok", proxy.getItem(1L));
            verify(ark).get("/base/items/1");
        }

        @Test
        void givenVoidReturn_thenCallsToBodilessEntity() {
            when(ark.delete("/base/items/1")).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.toBodilessEntity()).thenReturn(new ArkResponse<>(204, Map.of(), null));

            createProxy(ItemApi.class).deleteItem(1L);
            verify(clientResponse).toBodilessEntity();
        }

        @Test
        void givenArkResponse_thenCallsToEntity() {
            when(ark.get("/base/items/1")).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.toEntity(any(TypeRef.class))).thenReturn(new ArkResponse<>(200, Map.of(), "v"));

            ArkResponse<String> r = createProxy(ItemApi.class).getItemFull(1L);
            assertEquals(200, r.statusCode());
        }
    }

    @Nested
    class ObjectMethods {

        @Test
        void givenToString_thenReturnsProxyString() {
            ItemApi proxy = createProxy(ItemApi.class);
            assertTrue(proxy.toString().endsWith("@proxy"));
        }

        @Test
        void givenHashCode_thenReturnsIdentityHashCode() {
            ItemApi proxy = createProxy(ItemApi.class);
            assertEquals(System.identityHashCode(proxy), proxy.hashCode());
        }

        @Test
        @SuppressWarnings("all")
        void givenEquals_thenUsesIdentity() {
            ItemApi p1 = createProxy(ItemApi.class);
            ItemApi p2 = createProxy(ItemApi.class);
            assertTrue(p1.equals(p1));
            assertFalse(p1.equals(p2));
        }
    }
}
