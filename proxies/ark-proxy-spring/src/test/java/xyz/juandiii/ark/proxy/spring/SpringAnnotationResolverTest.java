package xyz.juandiii.ark.proxy.spring;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;
import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.core.proxy.AnnotationResolver;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class SpringAnnotationResolverTest {

    interface UserApi {
        @GetExchange("/users/{id}")
        String get(@PathVariable("id") String id);

        @GetExchange("/users/{userId}/posts/{postId}")
        String getPost(@PathVariable("userId") String userId, @PathVariable("postId") String postId);
    }

    @Test
    void givenNullPathVariable_whenResolving_thenThrowsArkException() throws NoSuchMethodException {
        SpringAnnotationResolver resolver = new SpringAnnotationResolver();
        Method method = UserApi.class.getMethod("get", String.class);

        ArkException ex = assertThrows(ArkException.class,
                () -> resolver.resolvePath("/users/{id}", method, new Object[]{null}));

        assertTrue(ex.getMessage().contains("id"),
                "Exception message must name the variable, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("null"),
                "Exception message must explain it's null, got: " + ex.getMessage());
    }

    @Test
    void givenNullPathVariableAmongValid_whenResolving_thenThrowsForTheNullOne() throws NoSuchMethodException {
        SpringAnnotationResolver resolver = new SpringAnnotationResolver();
        Method method = UserApi.class.getMethod("getPost", String.class, String.class);

        ArkException ex = assertThrows(ArkException.class,
                () -> resolver.resolvePath("/users/{userId}/posts/{postId}", method,
                        new Object[]{"42", null}));

        assertTrue(ex.getMessage().contains("postId"),
                "Exception message must name the null variable (postId), got: " + ex.getMessage());
    }

    @Test
    void givenAllArgsNull_whenResolving_thenReturnsPathUntouched() throws NoSuchMethodException {
        SpringAnnotationResolver resolver = new SpringAnnotationResolver();
        Method method = UserApi.class.getMethod("get", String.class);

        String result = resolver.resolvePath("/static", method, null);

        assertEquals("/static", result);
    }

    // ---- Content negotiation: accept (Accept header) on all verbs ----

    @Nested
    class AcceptResolution {

        interface AcceptVerbs {
            @GetExchange(value = "/g", accept = "application/json")
            String g();

            @PostExchange(value = "/p", accept = "application/xml")
            String p();

            @PutExchange(value = "/pu", accept = "text/plain")
            String pu();

            @PatchExchange(value = "/pa", accept = "application/cbor")
            String pa();

            @DeleteExchange(value = "/d", accept = "application/json")
            String d();
        }

        @Test
        void givenAcceptOnGetExchange_thenProducesIsSet() throws Exception {
            AnnotationResolver.MethodInfo info = new SpringAnnotationResolver()
                    .resolveMethod(AcceptVerbs.class.getMethod("g"));
            assertEquals("GET", info.httpMethod());
            assertEquals("application/json", info.produces());
        }

        @Test
        void givenAcceptOnPostExchange_thenProducesIsSet() throws Exception {
            AnnotationResolver.MethodInfo info = new SpringAnnotationResolver()
                    .resolveMethod(AcceptVerbs.class.getMethod("p"));
            assertEquals("POST", info.httpMethod());
            assertEquals("application/xml", info.produces());
        }

        @Test
        void givenAcceptOnPutExchange_thenProducesIsSet() throws Exception {
            AnnotationResolver.MethodInfo info = new SpringAnnotationResolver()
                    .resolveMethod(AcceptVerbs.class.getMethod("pu"));
            assertEquals("PUT", info.httpMethod());
            assertEquals("text/plain", info.produces());
        }

        @Test
        void givenAcceptOnPatchExchange_thenProducesIsSet() throws Exception {
            AnnotationResolver.MethodInfo info = new SpringAnnotationResolver()
                    .resolveMethod(AcceptVerbs.class.getMethod("pa"));
            assertEquals("PATCH", info.httpMethod());
            assertEquals("application/cbor", info.produces());
        }

        @Test
        void givenAcceptOnDeleteExchange_thenProducesIsSet() throws Exception {
            AnnotationResolver.MethodInfo info = new SpringAnnotationResolver()
                    .resolveMethod(AcceptVerbs.class.getMethod("d"));
            assertEquals("DELETE", info.httpMethod());
            assertEquals("application/json", info.produces());
        }
    }

    // ---- Class-level @HttpExchange fallback for accept + contentType ----

    @Nested
    class ClassLevelFallback {

        @HttpExchange(contentType = "application/json", accept = "application/json")
        interface ClassDefaults {
            @GetExchange("/x") String g();
            @PostExchange("/x") String p();
            @PostExchange(value = "/x", contentType = "application/xml", accept = "application/cbor")
            String pOverride();
        }

        @Test
        void givenClassLevelAccept_whenMethodDoesNotOverride_thenInherited() throws Exception {
            AnnotationResolver.MethodInfo info = new SpringAnnotationResolver()
                    .resolveMethod(ClassDefaults.class.getMethod("g"));
            assertEquals("application/json", info.produces());
        }

        @Test
        void givenClassLevelContentTypeAndAccept_whenPostNoOverride_thenInherited() throws Exception {
            AnnotationResolver.MethodInfo info = new SpringAnnotationResolver()
                    .resolveMethod(ClassDefaults.class.getMethod("p"));
            assertEquals("application/json", info.consumes());
            assertEquals("application/json", info.produces());
        }

        @Test
        void givenMethodOverridesClassLevel_thenMethodWins() throws Exception {
            AnnotationResolver.MethodInfo info = new SpringAnnotationResolver()
                    .resolveMethod(ClassDefaults.class.getMethod("pOverride"));
            assertEquals("application/xml", info.consumes());
            assertEquals("application/cbor", info.produces());
        }
    }

    // ---- PATCH contentType (previously ignored — regression guard) ----

    @Nested
    class PatchContentType {

        interface PatchApi {
            @PatchExchange(value = "/x", contentType = "application/merge-patch+json")
            String patch();
        }

        @Test
        void givenPatchExchangeWithContentType_thenConsumesIsSet() throws Exception {
            AnnotationResolver.MethodInfo info = new SpringAnnotationResolver()
                    .resolveMethod(PatchApi.class.getMethod("patch"));
            assertEquals("application/merge-patch+json", info.consumes());
        }
    }

    // ---- Absent accept stays null (regression guard) ----

    @Nested
    class NoAcceptSet {

        interface PlainApi {
            @GetExchange("/x") String g();
        }

        @Test
        void givenNoAcceptDeclared_thenProducesIsNull() throws Exception {
            AnnotationResolver.MethodInfo info = new SpringAnnotationResolver()
                    .resolveMethod(PlainApi.class.getMethod("g"));
            assertNull(info.produces());
        }
    }
}
