package xyz.juandiii.ark.proxy.jaxrs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.junit.jupiter.api.Test;
import xyz.juandiii.ark.core.exceptions.ArkException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class JaxRsAnnotationResolverTest {

    interface UserApi {
        @GET
        @Path("/users/{id}")
        String get(@PathParam("id") String id);

        @GET
        @Path("/users/{userId}/posts/{postId}")
        String getPost(@PathParam("userId") String userId, @PathParam("postId") String postId);
    }

    @Test
    void givenNullPathParam_whenResolving_thenThrowsArkException() throws NoSuchMethodException {
        JaxRsAnnotationResolver resolver = new JaxRsAnnotationResolver();
        Method method = UserApi.class.getMethod("get", String.class);

        ArkException ex = assertThrows(ArkException.class,
                () -> resolver.resolvePath("/users/{id}", method, new Object[]{null}));

        assertTrue(ex.getMessage().contains("id"),
                "Exception message must name the variable, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("null"),
                "Exception message must explain it's null, got: " + ex.getMessage());
    }

    @Test
    void givenNullPathParamAmongValid_whenResolving_thenThrowsForTheNullOne() throws NoSuchMethodException {
        JaxRsAnnotationResolver resolver = new JaxRsAnnotationResolver();
        Method method = UserApi.class.getMethod("getPost", String.class, String.class);

        ArkException ex = assertThrows(ArkException.class,
                () -> resolver.resolvePath("/users/{userId}/posts/{postId}", method,
                        new Object[]{"42", null}));

        assertTrue(ex.getMessage().contains("postId"),
                "Exception message must name the null variable (postId), got: " + ex.getMessage());
    }

    @Test
    void givenNullArgsArray_whenResolving_thenReturnsPathUntouched() throws NoSuchMethodException {
        JaxRsAnnotationResolver resolver = new JaxRsAnnotationResolver();
        Method method = UserApi.class.getMethod("get", String.class);

        String result = resolver.resolvePath("/static", method, null);

        assertEquals("/static", result);
    }
}
