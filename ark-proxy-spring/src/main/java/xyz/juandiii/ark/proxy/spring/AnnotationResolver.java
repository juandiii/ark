package xyz.juandiii.ark.proxy.spring;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;
import xyz.juandiii.ark.exceptions.ArkException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.regex.Pattern;

/**
 * Resolves HTTP method and path from Spring @HttpExchange annotations.
 *
 * @author Juan Diego Lopez V.
 */
final class AnnotationResolver {

    private static final Pattern UNRESOLVED_PLACEHOLDER = Pattern.compile("\\{[^}]+}");

    record MethodInfo(String httpMethod, String path) {}

    String resolveBasePath(Class<?> declaringClass) {
        HttpExchange exchange = declaringClass.getAnnotation(HttpExchange.class);
        if (exchange == null) return "";
        return resolve(exchange.url(), exchange.value());
    }

    MethodInfo resolveMethod(Method method) {
        GetExchange get = method.getAnnotation(GetExchange.class);
        if (get != null) return new MethodInfo("GET", resolve(get.url(), get.value()));

        PostExchange post = method.getAnnotation(PostExchange.class);
        if (post != null) return new MethodInfo("POST", resolve(post.url(), post.value()));

        PutExchange put = method.getAnnotation(PutExchange.class);
        if (put != null) return new MethodInfo("PUT", resolve(put.url(), put.value()));

        PatchExchange patch = method.getAnnotation(PatchExchange.class);
        if (patch != null) return new MethodInfo("PATCH", resolve(patch.url(), patch.value()));

        DeleteExchange delete = method.getAnnotation(DeleteExchange.class);
        if (delete != null) return new MethodInfo("DELETE", resolve(delete.url(), delete.value()));

        throw new ArkException("No @HttpExchange annotation found on method: " + method.getName());
    }

    private String resolve(String url, String value) {
        if (!url.isEmpty()) return url;
        if (!value.isEmpty()) return value;
        return "";
    }

    String resolvePath(String path, Method method, Object[] args) {
        if (args == null) return path;
        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
            PathVariable pv = params[i].getAnnotation(PathVariable.class);
            if (pv != null) {
                String name = pv.value().isEmpty() ? params[i].getName() : pv.value();
                path = path.replace("{" + name + "}", String.valueOf(args[i]));
            }
        }
        if (UNRESOLVED_PLACEHOLDER.matcher(path).find()) {
            throw new ArkException("Unresolved path variables in: " + path);
        }
        return path;
    }
}
