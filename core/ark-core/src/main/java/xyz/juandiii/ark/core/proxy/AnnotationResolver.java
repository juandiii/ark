package xyz.juandiii.ark.core.proxy;

import java.lang.reflect.Method;

/**
 * Resolves HTTP method, path, and content negotiation from framework-specific annotations.
 *
 * @author Juan Diego Lopez V.
 */
public interface AnnotationResolver {

    record MethodInfo(String httpMethod, String path, String consumes, String produces) {}

    String resolveBasePath(Class<?> declaringClass);

    MethodInfo resolveMethod(Method method);

    String resolvePath(String path, Method method, Object[] args);

    String resolveSubResourcePath(Method method, Object[] args);

    boolean isSubResource(Method method);
}
