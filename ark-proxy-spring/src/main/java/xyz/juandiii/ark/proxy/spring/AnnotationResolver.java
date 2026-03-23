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

final class AnnotationResolver {

    private static final Pattern UNRESOLVED_PLACEHOLDER = Pattern.compile("\\{[^}]+}");

    record MethodInfo(String httpMethod, String path) {}

    String resolveBasePath(Class<?> declaringClass) {
        HttpExchange exchange = declaringClass.getAnnotation(HttpExchange.class);
        return exchange != null ? exchange.url() : "";
    }

    MethodInfo resolveMethod(Method method) {
        GetExchange get = method.getAnnotation(GetExchange.class);
        if (get != null) return new MethodInfo("GET", get.url());

        PostExchange post = method.getAnnotation(PostExchange.class);
        if (post != null) return new MethodInfo("POST", post.url());

        PutExchange put = method.getAnnotation(PutExchange.class);
        if (put != null) return new MethodInfo("PUT", put.url());

        PatchExchange patch = method.getAnnotation(PatchExchange.class);
        if (patch != null) return new MethodInfo("PATCH", patch.url());

        DeleteExchange delete = method.getAnnotation(DeleteExchange.class);
        if (delete != null) return new MethodInfo("DELETE", delete.url());

        throw new ArkException("No @HttpExchange annotation found on method: " + method.getName());
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
