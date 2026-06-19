package xyz.juandiii.ark.proxy.spring;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import xyz.juandiii.ark.core.exceptions.ArkException;

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
}
