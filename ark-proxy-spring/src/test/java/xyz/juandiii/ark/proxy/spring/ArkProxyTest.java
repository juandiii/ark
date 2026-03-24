package xyz.juandiii.ark.proxy.spring;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import xyz.juandiii.ark.Ark;
import xyz.juandiii.ark.exceptions.ArkException;
import xyz.juandiii.ark.http.ArkResponse;
import xyz.juandiii.ark.http.ClientRequest;
import xyz.juandiii.ark.http.ClientResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArkProxyTest {

    @Mock
    Ark ark;

    @Mock
    ClientRequest clientRequest;

    @Mock
    ClientResponse clientResponse;

    // ---- Test interfaces (using url = ...) ----

    @HttpExchange(url = "/users")
    interface UserApi {

        @GetExchange(url = "/{id}")
        String findById(@PathVariable("id") Long id);

        @GetExchange
        String findByName(@RequestParam("name") String name);

        @PostExchange
        void create(@RequestBody Object body);

        @GetExchange(url = "/sub")
        SubResourceApi subResource();
    }

    @HttpExchange(url = "/details")
    interface SubResourceApi {

        @GetExchange(url = "/{id}")
        String detail(@PathVariable("id") Long id);
    }

    // ---- Test interfaces (using value shorthand) ----

    @HttpExchange("/orders")
    interface OrderApi {

        @GetExchange("/{id}")
        String findById(@PathVariable("id") Long id);

        @PostExchange("/create")
        void create(@RequestBody Object body);
    }

    static class NotAnInterface {}

    @Nested
    class Create {

        @Test
        void givenInterface_whenCreate_thenReturnsProxy() {
            UserApi proxy = ArkProxy.create(UserApi.class, ark);

            assertNotNull(proxy);
            assertTrue(proxy instanceof UserApi);
        }

        @Test
        void givenNonInterface_whenCreate_thenThrowsArkException() {
            assertThrows(ArkException.class, () ->
                    ArkProxy.create(NotAnInterface.class, ark));
        }
    }

    @Nested
    class HttpMethodDispatch {

        @Test
        void givenGetExchange_whenInvoked_thenCallsArkGet() {
            when(ark.get("/users/1")).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.body(any(xyz.juandiii.ark.TypeRef.class))).thenReturn("Juan");

            UserApi proxy = ArkProxy.create(UserApi.class, ark);
            String result = proxy.findById(1L);

            assertEquals("Juan", result);
            verify(ark).get("/users/1");
        }

        @Test
        void givenPostExchange_whenInvoked_thenCallsArkPost() {
            when(ark.post("/users")).thenReturn(clientRequest);
            when(clientRequest.body(any())).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.toBodilessEntity())
                    .thenReturn(new ArkResponse<>(201, Map.of(), null));

            UserApi proxy = ArkProxy.create(UserApi.class, ark);
            proxy.create(Map.of("name", "Juan"));

            verify(ark).post("/users");
        }
    }

    @Nested
    class ParameterBinding {

        @Test
        void givenPathVariable_whenInvoked_thenResolvesPath() {
            when(ark.get("/users/42")).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.body(any(xyz.juandiii.ark.TypeRef.class))).thenReturn("found");

            UserApi proxy = ArkProxy.create(UserApi.class, ark);
            String result = proxy.findById(42L);

            assertEquals("found", result);
            verify(ark).get("/users/42");
        }

        @Test
        void givenQueryParam_whenInvoked_thenAddsQueryParam() {
            when(ark.get("/users")).thenReturn(clientRequest);
            when(clientRequest.queryParam(eq("name"), eq("Juan"))).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.body(any(xyz.juandiii.ark.TypeRef.class))).thenReturn("Juan");

            UserApi proxy = ArkProxy.create(UserApi.class, ark);
            String result = proxy.findByName("Juan");

            assertEquals("Juan", result);
            verify(clientRequest).queryParam("name", "Juan");
        }

        @Test
        void givenRequestBody_whenInvoked_thenSetsBody() {
            Object payload = Map.of("name", "Juan");
            when(ark.post("/users")).thenReturn(clientRequest);
            when(clientRequest.body(payload)).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.toBodilessEntity())
                    .thenReturn(new ArkResponse<>(201, Map.of(), null));

            UserApi proxy = ArkProxy.create(UserApi.class, ark);
            proxy.create(payload);

            verify(clientRequest).body(payload);
        }
    }

    @Nested
    class ValueShorthand {

        @Test
        void givenValueOnHttpExchange_whenInvoked_thenResolvesBasePath() {
            when(ark.get("/orders/1")).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.body(any(xyz.juandiii.ark.TypeRef.class))).thenReturn("order");

            OrderApi proxy = ArkProxy.create(OrderApi.class, ark);
            String result = proxy.findById(1L);

            assertEquals("order", result);
            verify(ark).get("/orders/1");
        }

        @Test
        void givenValueOnPostExchange_whenInvoked_thenResolvesMethodPath() {
            when(ark.post("/orders/create")).thenReturn(clientRequest);
            when(clientRequest.body(any())).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.toBodilessEntity())
                    .thenReturn(new ArkResponse<>(201, Map.of(), null));

            OrderApi proxy = ArkProxy.create(OrderApi.class, ark);
            proxy.create(Map.of("item", "laptop"));

            verify(ark).post("/orders/create");
        }
    }

    @Nested
    class VoidReturn {

        @Test
        void givenVoidReturn_whenInvoked_thenCallsToBodilessEntity() {
            when(ark.post("/users")).thenReturn(clientRequest);
            when(clientRequest.body(any())).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.toBodilessEntity())
                    .thenReturn(new ArkResponse<>(201, Map.of(), null));

            UserApi proxy = ArkProxy.create(UserApi.class, ark);
            proxy.create("payload");

            verify(clientResponse).toBodilessEntity();
        }
    }

    @Nested
    class SubResource {

        @Test
        void givenSubResource_whenInvoked_thenCreatesSubProxy() {
            UserApi proxy = ArkProxy.create(UserApi.class, ark);
            SubResourceApi subProxy = proxy.subResource();

            assertNotNull(subProxy);
            assertTrue(subProxy instanceof SubResourceApi);
        }
    }

    @Nested
    class ObjectMethods {

        @Test
        void givenObjectMethods_whenToString_thenReturnsProxyString() {
            UserApi proxy = ArkProxy.create(UserApi.class, ark);
            String result = proxy.toString();

            assertTrue(result.contains(UserApi.class.getName()));
            assertTrue(result.endsWith("@proxy"));
        }

        @Test
        void givenObjectMethods_whenHashCode_thenReturnsIdentityHashCode() {
            UserApi proxy = ArkProxy.create(UserApi.class, ark);
            int expected = System.identityHashCode(proxy);

            assertEquals(expected, proxy.hashCode());
        }

        @Test
        @SuppressWarnings("all")
        void givenObjectMethods_whenEquals_thenUsesIdentity() {
            UserApi proxy1 = ArkProxy.create(UserApi.class, ark);
            UserApi proxy2 = ArkProxy.create(UserApi.class, ark);

            assertTrue(proxy1.equals(proxy1));
            assertFalse(proxy1.equals(proxy2));
            assertFalse(proxy1.equals(null));
        }
    }
}
