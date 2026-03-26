package xyz.juandiii.ark.proxy.jaxrs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.smallrye.mutiny.Uni;
import xyz.juandiii.ark.Ark;
import xyz.juandiii.ark.async.AsyncArk;
import xyz.juandiii.ark.async.http.AsyncClientRequest;
import xyz.juandiii.ark.async.http.AsyncClientResponse;
import xyz.juandiii.ark.exceptions.ArkException;
import xyz.juandiii.ark.http.ArkResponse;
import xyz.juandiii.ark.http.ClientRequest;
import xyz.juandiii.ark.http.ClientResponse;
import xyz.juandiii.ark.mutiny.MutinyArk;
import xyz.juandiii.ark.mutiny.http.MutinyClientRequest;
import xyz.juandiii.ark.mutiny.http.MutinyClientResponse;
import xyz.juandiii.ark.proxy.ArkProxy;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArkJaxRsProxyTest {

    @Mock Ark ark;
    @Mock ClientRequest clientRequest;
    @Mock ClientResponse clientResponse;
    @Mock AsyncArk asyncArk;
    @Mock AsyncClientRequest asyncClientRequest;
    @Mock AsyncClientResponse asyncClientResponse;
    @Mock MutinyArk mutinyArk;
    @Mock MutinyClientRequest mutinyClientRequest;
    @Mock MutinyClientResponse mutinyClientResponse;

    // ---- Test interfaces ----

    @Path("/users")
    @Produces("application/json")
    @Consumes("application/json")
    interface UserApi {

        @GET
        @Path("/{id}")
        String findById(@PathParam("id") Long id);

        @GET
        String findByName(@QueryParam("name") String name);

        @POST
        void create(Object body);

        @Path("/sub")
        SubResourceApi subResource();
    }

    @Path("/details")
    interface SubResourceApi {

        @GET
        @Path("/{id}")
        String detail(@PathParam("id") Long id);
    }

    @Path("/orders")
    interface OrderApi {

        @GET
        @Path("/{id}")
        String findById(@PathParam("id") Long id);

        @POST
        @Path("/create")
        void create(Object body);
    }

    @Path("/api")
    @Consumes("application/json")
    @Produces("application/json")
    interface FullApi {

        @PUT
        @Path("/{id}")
        String update(@PathParam("id") Long id, Object body);

        @PATCH
        @Path("/{id}")
        String patch(@PathParam("id") Long id, Object body);

        @DELETE
        @Path("/{id}")
        void delete(@PathParam("id") Long id);

        @GET
        @Path("/{id}")
        ArkResponse<String> findByIdFull(@PathParam("id") Long id);

        @POST
        void createWithHeader(@HeaderParam("X-Token") String token, Object body);

        @POST
        @Path("/form")
        void submitForm(@FormParam("key") String key, @FormParam("value") String value);

        @POST
        void implicitBody(Object body);

        @GET
        @Path("/{id: [0-9]+}")
        String findByRegexPath(@PathParam("id") Long id);

        @POST
        @Consumes("text/plain")
        @Produces("text/xml")
        String overrideContentType(String body);
    }

    interface NoPathApi {

        @GET
        @Path("/health")
        String health();
    }

    @Path("/async-users")
    interface AsyncUserApi {

        @GET
        @Path("/{id}")
        CompletableFuture<String> findById(@PathParam("id") Long id);

        @POST
        CompletableFuture<Void> create(Object body);

        @PUT
        @Path("/{id}")
        CompletableFuture<String> update(@PathParam("id") Long id, Object body);

        @PATCH
        @Path("/{id}")
        CompletableFuture<String> patch(@PathParam("id") Long id, Object body);

        @DELETE
        @Path("/{id}")
        CompletableFuture<Void> delete(@PathParam("id") Long id);
    }

    @Path("/mutiny-users")
    interface MutinyUserApi {

        @GET
        @Path("/{id}")
        Uni<String> findById(@PathParam("id") Long id);

        @POST
        Uni<Void> create(Object body);

        @PUT
        @Path("/{id}")
        Uni<String> update(@PathParam("id") Long id, Object body);

        @PATCH
        @Path("/{id}")
        Uni<String> patch(@PathParam("id") Long id, Object body);

        @DELETE
        @Path("/{id}")
        Uni<Void> delete(@PathParam("id") Long id);
    }

    interface NoAnnotationApi {
        String noAnnotation();
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
        void givenGet_whenInvoked_thenCallsArkGet() {
            when(ark.get("/users/1")).thenReturn(clientRequest);
            when(clientRequest.accept(anyString())).thenReturn(clientRequest);
            when(clientRequest.contentType(anyString())).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.body(any(xyz.juandiii.ark.TypeRef.class))).thenReturn("Juan");

            UserApi proxy = ArkProxy.create(UserApi.class, ark);
            String result = proxy.findById(1L);

            assertEquals("Juan", result);
            verify(ark).get("/users/1");
        }

        @Test
        void givenPost_whenInvoked_thenCallsArkPost() {
            when(ark.post("/users")).thenReturn(clientRequest);
            when(clientRequest.accept(anyString())).thenReturn(clientRequest);
            when(clientRequest.contentType(anyString())).thenReturn(clientRequest);
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
        void givenPathParam_whenInvoked_thenResolvesPath() {
            when(ark.get("/users/42")).thenReturn(clientRequest);
            when(clientRequest.accept(anyString())).thenReturn(clientRequest);
            when(clientRequest.contentType(anyString())).thenReturn(clientRequest);
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
            when(clientRequest.accept(anyString())).thenReturn(clientRequest);
            when(clientRequest.contentType(anyString())).thenReturn(clientRequest);
            when(clientRequest.queryParam(eq("name"), eq("Juan"))).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.body(any(xyz.juandiii.ark.TypeRef.class))).thenReturn("Juan");

            UserApi proxy = ArkProxy.create(UserApi.class, ark);
            String result = proxy.findByName("Juan");

            assertEquals("Juan", result);
            verify(clientRequest).queryParam("name", "Juan");
        }

        @Test
        void givenImplicitBody_whenInvoked_thenSetsBody() {
            Object payload = Map.of("name", "Juan");
            when(ark.post("/users")).thenReturn(clientRequest);
            when(clientRequest.accept(anyString())).thenReturn(clientRequest);
            when(clientRequest.contentType(anyString())).thenReturn(clientRequest);
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
    class AllHttpMethods {

        @Test
        void givenPut_whenInvoked_thenCallsArkPut() {
            when(ark.put("/api/1")).thenReturn(clientRequest);
            when(clientRequest.accept(anyString())).thenReturn(clientRequest);
            when(clientRequest.contentType(anyString())).thenReturn(clientRequest);
            when(clientRequest.body(any())).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.body(any(xyz.juandiii.ark.TypeRef.class))).thenReturn("updated");

            FullApi proxy = ArkProxy.create(FullApi.class, ark);
            assertEquals("updated", proxy.update(1L, "payload"));
            verify(ark).put("/api/1");
        }

        @Test
        void givenPatch_whenInvoked_thenCallsArkPatch() {
            when(ark.patch("/api/1")).thenReturn(clientRequest);
            when(clientRequest.accept(anyString())).thenReturn(clientRequest);
            when(clientRequest.contentType(anyString())).thenReturn(clientRequest);
            when(clientRequest.body(any())).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.body(any(xyz.juandiii.ark.TypeRef.class))).thenReturn("patched");

            FullApi proxy = ArkProxy.create(FullApi.class, ark);
            assertEquals("patched", proxy.patch(1L, "payload"));
            verify(ark).patch("/api/1");
        }

        @Test
        void givenDelete_whenInvoked_thenCallsArkDelete() {
            when(ark.delete("/api/1")).thenReturn(clientRequest);
            when(clientRequest.accept(anyString())).thenReturn(clientRequest);
            when(clientRequest.contentType(anyString())).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.toBodilessEntity())
                    .thenReturn(new ArkResponse<>(204, Map.of(), null));

            FullApi proxy = ArkProxy.create(FullApi.class, ark);
            proxy.delete(1L);
            verify(ark).delete("/api/1");
        }
    }

    @Nested
    class ArkResponseReturn {

        @Test
        void givenArkResponseReturnType_whenInvoked_thenCallsToEntity() {
            when(ark.get("/api/1")).thenReturn(clientRequest);
            when(clientRequest.accept(anyString())).thenReturn(clientRequest);
            when(clientRequest.contentType(anyString())).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.toEntity(any(xyz.juandiii.ark.TypeRef.class)))
                    .thenReturn(new ArkResponse<>(200, Map.of(), "Juan"));

            FullApi proxy = ArkProxy.create(FullApi.class, ark);
            ArkResponse<String> result = proxy.findByIdFull(1L);

            assertEquals(200, result.statusCode());
            assertEquals("Juan", result.body());
            verify(clientResponse).toEntity(any(xyz.juandiii.ark.TypeRef.class));
        }
    }

    @Nested
    class RequestHeaderBinding {

        @Test
        void givenHeaderParam_whenInvoked_thenSetsHeader() {
            when(ark.post("/api")).thenReturn(clientRequest);
            when(clientRequest.accept(anyString())).thenReturn(clientRequest);
            when(clientRequest.contentType(anyString())).thenReturn(clientRequest);
            when(clientRequest.header(eq("X-Token"), eq("secret"))).thenReturn(clientRequest);
            when(clientRequest.body(any())).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.toBodilessEntity())
                    .thenReturn(new ArkResponse<>(201, Map.of(), null));

            FullApi proxy = ArkProxy.create(FullApi.class, ark);
            proxy.createWithHeader("secret", Map.of("name", "Juan"));

            verify(clientRequest).header("X-Token", "secret");
        }
    }

    @Nested
    class FormData {

        @Test
        void givenFormParam_whenInvoked_thenEncodesAsForm() {
            when(ark.post("/api/form")).thenReturn(clientRequest);
            when(clientRequest.accept(anyString())).thenReturn(clientRequest);
            when(clientRequest.contentType(anyString())).thenReturn(clientRequest);
            when(clientRequest.body(any(String.class))).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.toBodilessEntity())
                    .thenReturn(new ArkResponse<>(200, Map.of(), null));

            FullApi proxy = ArkProxy.create(FullApi.class, ark);
            proxy.submitForm("myKey", "myValue");

            verify(clientRequest).contentType("application/x-www-form-urlencoded");
            verify(clientRequest).body(argThat((String s) ->
                    s.contains("key=myKey") && s.contains("value=myValue")));
        }
    }

    @Nested
    class ImplicitBody {

        @Test
        void givenNoAnnotation_whenInvoked_thenTreatsAsBody() {
            when(ark.post("/api")).thenReturn(clientRequest);
            when(clientRequest.accept(anyString())).thenReturn(clientRequest);
            when(clientRequest.contentType(anyString())).thenReturn(clientRequest);
            when(clientRequest.body(eq("implicit"))).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.toBodilessEntity())
                    .thenReturn(new ArkResponse<>(201, Map.of(), null));

            FullApi proxy = ArkProxy.create(FullApi.class, ark);
            proxy.implicitBody("implicit");

            verify(clientRequest).body("implicit");
        }
    }

    @Nested
    class VoidReturn {

        @Test
        void givenVoidReturn_whenInvoked_thenCallsToBodilessEntity() {
            when(ark.post("/users")).thenReturn(clientRequest);
            when(clientRequest.accept(anyString())).thenReturn(clientRequest);
            when(clientRequest.contentType(anyString())).thenReturn(clientRequest);
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
    class ContentNegotiation {

        @Test
        void givenConsumesAndProduces_whenInvoked_thenSetsHeaders() {
            when(ark.get("/users/1")).thenReturn(clientRequest);
            when(clientRequest.contentType("application/json")).thenReturn(clientRequest);
            when(clientRequest.accept("application/json")).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.body(any(xyz.juandiii.ark.TypeRef.class))).thenReturn("Juan");

            UserApi proxy = ArkProxy.create(UserApi.class, ark);
            proxy.findById(1L);

            verify(clientRequest).contentType("application/json");
            verify(clientRequest).accept("application/json");
        }

        @Test
        void givenMethodLevelOverride_whenInvoked_thenUsesMethodLevel() {
            when(ark.post("/api")).thenReturn(clientRequest);
            when(clientRequest.contentType("text/plain")).thenReturn(clientRequest);
            when(clientRequest.accept("text/xml")).thenReturn(clientRequest);
            when(clientRequest.body(any())).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.body(any(xyz.juandiii.ark.TypeRef.class))).thenReturn("result");

            FullApi proxy = ArkProxy.create(FullApi.class, ark);
            proxy.overrideContentType("data");

            verify(clientRequest).contentType("text/plain");
            verify(clientRequest).accept("text/xml");
        }
    }

    @Nested
    class RegexPathTemplate {

        @Test
        void givenRegexPath_whenInvoked_thenNormalizesAndResolves() {
            when(ark.get("/api/42")).thenReturn(clientRequest);
            when(clientRequest.accept(anyString())).thenReturn(clientRequest);
            when(clientRequest.contentType(anyString())).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.body(any(xyz.juandiii.ark.TypeRef.class))).thenReturn("found");

            FullApi proxy = ArkProxy.create(FullApi.class, ark);
            String result = proxy.findByRegexPath(42L);

            assertEquals("found", result);
            verify(ark).get("/api/42");
        }
    }

    @Nested
    class NoPathOnClass {

        @Test
        void givenNoPathOnInterface_whenInvoked_thenUsesMethodPath() {
            when(ark.get("/health")).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.body(any(xyz.juandiii.ark.TypeRef.class))).thenReturn("ok");

            NoPathApi proxy = ArkProxy.create(NoPathApi.class, ark);
            String result = proxy.health();

            assertEquals("ok", result);
            verify(ark).get("/health");
        }
    }

    @Nested
    class NoAnnotation {

        @Test
        void givenNoAnnotation_whenCreate_thenThrowsArkException() {
            assertThrows(ArkException.class, () -> ArkProxy.create(NoAnnotationApi.class, ark));
        }
    }

    @Nested
    class AsyncProxy {

        @Test
        void givenAsyncArk_whenGetInvoked_thenCallsAsyncArkGet() {
            CompletableFuture<String> expected = CompletableFuture.completedFuture("Juan");
            when(asyncArk.get("/async-users/1")).thenReturn(asyncClientRequest);
            when(asyncClientRequest.retrieve()).thenReturn(asyncClientResponse);
            when(asyncClientResponse.body(any(xyz.juandiii.ark.TypeRef.class))).thenReturn(expected);

            AsyncUserApi proxy = ArkProxy.create(AsyncUserApi.class, asyncArk);
            CompletableFuture<String> result = proxy.findById(1L);

            assertSame(expected, result);
            verify(asyncArk).get("/async-users/1");
        }

        @Test
        void givenAsyncArk_whenPostInvoked_thenCallsAsyncArkPost() {
            CompletableFuture<ArkResponse<Void>> expected =
                    CompletableFuture.completedFuture(new ArkResponse<>(201, Map.of(), null));
            when(asyncArk.post("/async-users")).thenReturn(asyncClientRequest);
            when(asyncClientRequest.body(any())).thenReturn(asyncClientRequest);
            when(asyncClientRequest.retrieve()).thenReturn(asyncClientResponse);
            when(asyncClientResponse.toBodilessEntity()).thenReturn(expected);

            AsyncUserApi proxy = ArkProxy.create(AsyncUserApi.class, asyncArk);
            proxy.create("payload");

            verify(asyncArk).post("/async-users");
        }

        @Test
        void givenAsyncArk_whenPutInvoked_thenCallsAsyncArkPut() {
            CompletableFuture<String> expected = CompletableFuture.completedFuture("updated");
            when(asyncArk.put("/async-users/1")).thenReturn(asyncClientRequest);
            when(asyncClientRequest.body(any())).thenReturn(asyncClientRequest);
            when(asyncClientRequest.retrieve()).thenReturn(asyncClientResponse);
            when(asyncClientResponse.body(any(xyz.juandiii.ark.TypeRef.class))).thenReturn(expected);

            AsyncUserApi proxy = ArkProxy.create(AsyncUserApi.class, asyncArk);
            proxy.update(1L, "body");
            verify(asyncArk).put("/async-users/1");
        }

        @Test
        void givenAsyncArk_whenPatchInvoked_thenCallsAsyncArkPatch() {
            CompletableFuture<String> expected = CompletableFuture.completedFuture("patched");
            when(asyncArk.patch("/async-users/1")).thenReturn(asyncClientRequest);
            when(asyncClientRequest.body(any())).thenReturn(asyncClientRequest);
            when(asyncClientRequest.retrieve()).thenReturn(asyncClientResponse);
            when(asyncClientResponse.body(any(xyz.juandiii.ark.TypeRef.class))).thenReturn(expected);

            AsyncUserApi proxy = ArkProxy.create(AsyncUserApi.class, asyncArk);
            proxy.patch(1L, "body");
            verify(asyncArk).patch("/async-users/1");
        }

        @Test
        void givenAsyncArk_whenDeleteInvoked_thenCallsAsyncArkDelete() {
            CompletableFuture<ArkResponse<Void>> expected =
                    CompletableFuture.completedFuture(new ArkResponse<>(204, Map.of(), null));
            when(asyncArk.delete("/async-users/1")).thenReturn(asyncClientRequest);
            when(asyncClientRequest.retrieve()).thenReturn(asyncClientResponse);
            when(asyncClientResponse.toBodilessEntity()).thenReturn(expected);

            AsyncUserApi proxy = ArkProxy.create(AsyncUserApi.class, asyncArk);
            proxy.delete(1L);
            verify(asyncArk).delete("/async-users/1");
        }
    }

    @Nested
    class MutinyProxy {

        @Test
        void givenMutinyArk_whenGetInvoked_thenCallsMutinyArkGet() {
            Uni<String> expected = Uni.createFrom().item("Juan");
            when(mutinyArk.get("/mutiny-users/1")).thenReturn(mutinyClientRequest);
            when(mutinyClientRequest.retrieve()).thenReturn(mutinyClientResponse);
            when(mutinyClientResponse.body(any(xyz.juandiii.ark.TypeRef.class))).thenReturn(expected);

            MutinyUserApi proxy = ArkProxy.create(MutinyUserApi.class, mutinyArk);
            Uni<String> result = proxy.findById(1L);

            assertSame(expected, result);
            verify(mutinyArk).get("/mutiny-users/1");
        }

        @Test
        void givenMutinyArk_whenPostInvoked_thenCallsMutinyArkPost() {
            Uni<ArkResponse<Void>> expected =
                    Uni.createFrom().item(new ArkResponse<>(201, Map.of(), null));
            when(mutinyArk.post("/mutiny-users")).thenReturn(mutinyClientRequest);
            when(mutinyClientRequest.body(any())).thenReturn(mutinyClientRequest);
            when(mutinyClientRequest.retrieve()).thenReturn(mutinyClientResponse);
            when(mutinyClientResponse.toBodilessEntity()).thenReturn(expected);

            MutinyUserApi proxy = ArkProxy.create(MutinyUserApi.class, mutinyArk);
            proxy.create("payload");

            verify(mutinyArk).post("/mutiny-users");
        }

        @Test
        void givenMutinyArk_whenPutInvoked_thenCallsMutinyArkPut() {
            Uni<String> expected = Uni.createFrom().item("updated");
            when(mutinyArk.put("/mutiny-users/1")).thenReturn(mutinyClientRequest);
            when(mutinyClientRequest.body(any())).thenReturn(mutinyClientRequest);
            when(mutinyClientRequest.retrieve()).thenReturn(mutinyClientResponse);
            when(mutinyClientResponse.body(any(xyz.juandiii.ark.TypeRef.class))).thenReturn(expected);

            MutinyUserApi proxy = ArkProxy.create(MutinyUserApi.class, mutinyArk);
            proxy.update(1L, "body");
            verify(mutinyArk).put("/mutiny-users/1");
        }

        @Test
        void givenMutinyArk_whenPatchInvoked_thenCallsMutinyArkPatch() {
            Uni<String> expected = Uni.createFrom().item("patched");
            when(mutinyArk.patch("/mutiny-users/1")).thenReturn(mutinyClientRequest);
            when(mutinyClientRequest.body(any())).thenReturn(mutinyClientRequest);
            when(mutinyClientRequest.retrieve()).thenReturn(mutinyClientResponse);
            when(mutinyClientResponse.body(any(xyz.juandiii.ark.TypeRef.class))).thenReturn(expected);

            MutinyUserApi proxy = ArkProxy.create(MutinyUserApi.class, mutinyArk);
            proxy.patch(1L, "body");
            verify(mutinyArk).patch("/mutiny-users/1");
        }

        @Test
        void givenMutinyArk_whenDeleteInvoked_thenCallsMutinyArkDelete() {
            Uni<ArkResponse<Void>> expected =
                    Uni.createFrom().item(new ArkResponse<>(204, Map.of(), null));
            when(mutinyArk.delete("/mutiny-users/1")).thenReturn(mutinyClientRequest);
            when(mutinyClientRequest.retrieve()).thenReturn(mutinyClientResponse);
            when(mutinyClientResponse.toBodilessEntity()).thenReturn(expected);

            MutinyUserApi proxy = ArkProxy.create(MutinyUserApi.class, mutinyArk);
            proxy.delete(1L);
            verify(mutinyArk).delete("/mutiny-users/1");
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
