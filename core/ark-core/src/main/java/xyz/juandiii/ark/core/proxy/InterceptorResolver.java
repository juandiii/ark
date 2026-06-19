package xyz.juandiii.ark.core.proxy;

import xyz.juandiii.ark.core.AbstractArkBuilder;
import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.core.interceptor.RequestInterceptor;
import xyz.juandiii.ark.core.interceptor.ResponseInterceptor;

import java.util.Map;
import java.util.function.Function;

/**
 * Shared utility for resolving and applying interceptors to builders.
 *
 * @author Juan Diego Lopez V.
 */
public final class InterceptorResolver {

    private InterceptorResolver() {}

    public static Object resolve(Class<?> clazz, Function<Class<?>, Object> containerLookup) {
        try {
            Object bean = containerLookup.apply(clazz);
            if (bean != null) return bean;
        } catch (Exception ignored) {}
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new ArkException("Failed to create interceptor: " + clazz.getName(), e);
        }
    }

    public static <B extends AbstractArkBuilder<B>> void applyHeaders(B builder, Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            builder.requestInterceptor(ctx -> headers.forEach(ctx::header));
        }
    }

    public static <B extends AbstractArkBuilder<B>> void applyInterceptors(
            B builder, Class<?>[] interceptorClasses, Function<Class<?>, Object> containerLookup) {
        for (Class<?> interceptorClass : interceptorClasses) {
            Object interceptor = resolve(interceptorClass, containerLookup);
            if (interceptor instanceof RequestInterceptor req) {
                builder.requestInterceptor(req);
            }
            if (interceptor instanceof ResponseInterceptor res) {
                builder.responseInterceptor(res);
            }
        }
    }
}