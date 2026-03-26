package xyz.juandiii.ark.proxy.jaxrs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import xyz.juandiii.ark.exceptions.ArkException;
import xyz.juandiii.ark.proxy.AnnotationResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.regex.Pattern;

/**
 * Resolves HTTP method and path from JAX-RS annotations.
 *
 * @author Juan Diego Lopez V.
 */
final class JaxRsAnnotationResolver implements AnnotationResolver {

    private static final Pattern UNRESOLVED_PLACEHOLDER = Pattern.compile("\\{[^}]+}");
    private static final Pattern REGEX_TEMPLATE = Pattern.compile("\\{(\\w+)\\s*:[^}]*}");

    @Override
    public String resolveBasePath(Class<?> declaringClass) {
        Path path = declaringClass.getAnnotation(Path.class);
        if (path != null) return path.value();

        for (Class<?> parent : declaringClass.getInterfaces()) {
            Path parentPath = parent.getAnnotation(Path.class);
            if (parentPath != null) return parentPath.value();
        }
        return "";
    }

    @Override
    public MethodInfo resolveMethod(Method method) {
        String httpMethod = resolveHttpMethod(method);
        if (httpMethod == null) {
            throw new ArkException("No JAX-RS HTTP method annotation found on method: " + method.getName());
        }

        Path methodPath = method.getAnnotation(Path.class);
        String path = methodPath != null ? methodPath.value() : "";

        String consumes = resolveConsumes(method);
        String produces = resolveProduces(method);

        return new MethodInfo(httpMethod, path, consumes, produces);
    }

    @Override
    public String resolvePath(String path, Method method, Object[] args) {
        if (args == null) return normalizeRegexTemplates(path);

        path = normalizeRegexTemplates(path);

        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
            PathParam pp = params[i].getAnnotation(PathParam.class);
            if (pp != null) {
                path = path.replace("{" + pp.value() + "}", String.valueOf(args[i]));
            }
        }
        if (UNRESOLVED_PLACEHOLDER.matcher(path).find()) {
            throw new ArkException("Unresolved path variables in: " + path);
        }
        return path;
    }

    @Override
    public String resolveSubResourcePath(Method method, Object[] args) {
        Path methodPath = method.getAnnotation(Path.class);
        String path = methodPath != null ? methodPath.value() : "";
        return resolvePath(path, method, args);
    }

    private String resolveHttpMethod(Method method) {
        if (method.isAnnotationPresent(GET.class)) return "GET";
        if (method.isAnnotationPresent(POST.class)) return "POST";
        if (method.isAnnotationPresent(PUT.class)) return "PUT";
        if (method.isAnnotationPresent(PATCH.class)) return "PATCH";
        if (method.isAnnotationPresent(DELETE.class)) return "DELETE";
        return null;
    }

    @Override
    public boolean isSubResource(Method method) {
        Class<?> returnType = method.getReturnType();
        return returnType.isInterface()
                && returnType.isAnnotationPresent(Path.class)
                && resolveHttpMethod(method) == null;
    }

    private String resolveConsumes(Method method) {
        Consumes methodConsumes = method.getAnnotation(Consumes.class);
        if (methodConsumes != null && methodConsumes.value().length > 0) {
            return methodConsumes.value()[0];
        }

        Consumes classConsumes = method.getDeclaringClass().getAnnotation(Consumes.class);
        if (classConsumes != null && classConsumes.value().length > 0) {
            return classConsumes.value()[0];
        }
        return null;
    }

    private String resolveProduces(Method method) {
        Produces methodProduces = method.getAnnotation(Produces.class);
        if (methodProduces != null && methodProduces.value().length > 0) {
            return methodProduces.value()[0];
        }

        Produces classProduces = method.getDeclaringClass().getAnnotation(Produces.class);
        if (classProduces != null && classProduces.value().length > 0) {
            return classProduces.value()[0];
        }
        return null;
    }

    private String normalizeRegexTemplates(String path) {
        return REGEX_TEMPLATE.matcher(path).replaceAll("{$1}");
    }
}
