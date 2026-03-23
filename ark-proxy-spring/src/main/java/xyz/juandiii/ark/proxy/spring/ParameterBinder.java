package xyz.juandiii.ark.proxy.spring;

import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;
import xyz.juandiii.ark.http.ClientRequest;
import xyz.juandiii.ark.type.MediaType;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Binds method parameters to HTTP request components.
 *
 * @author Juan Diego Lopez V.
 */
final class ParameterBinder {

    void apply(ClientRequest request, Method method, Object[] args) {
        if (args == null) return;
        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
            if (params[i].isAnnotationPresent(PathVariable.class)) {
                continue;
            }
            if (applyQueryParam(request, params[i], args[i])) continue;
            if (applyHeader(request, params[i], args[i])) continue;
            if (params[i].isAnnotationPresent(RequestBody.class) || isImplicitBodyParam(params[i])) {
                applyBody(request, method, args[i]);
            }
        }
    }

    private boolean applyQueryParam(ClientRequest request, Parameter param, Object value) {
        RequestParam rp = param.getAnnotation(RequestParam.class);
        if (rp == null) return false;
        String name = rp.value().isEmpty() ? param.getName() : rp.value();
        if (value != null) request.queryParam(name, String.valueOf(value));
        return true;
    }

    private boolean applyHeader(ClientRequest request, Parameter param, Object value) {
        RequestHeader rh = param.getAnnotation(RequestHeader.class);
        if (rh == null) return false;
        String name = rh.value().isEmpty() ? param.getName() : rh.value();
        if (value != null) request.header(name, String.valueOf(value));
        return true;
    }

    private void applyBody(ClientRequest request, Method method, Object body) {
        if (body instanceof MultiValueMap<?, ?> formData) {
            request.contentType(MediaType.APPLICATION_FORM_URLENCODED);
            request.body(encodeFormData(formData));
        } else if (body instanceof Map<?, ?> map && isFormContentType(method)) {
            request.contentType(MediaType.APPLICATION_FORM_URLENCODED);
            request.body(encodeMap(map));
        } else {
            request.body(body);
        }
    }

    private boolean isImplicitBodyParam(Parameter param) {
        return !param.isAnnotationPresent(PathVariable.class)
                && !param.isAnnotationPresent(RequestParam.class)
                && !param.isAnnotationPresent(RequestHeader.class)
                && !param.isAnnotationPresent(RequestBody.class);
    }

    private boolean isFormContentType(Method method) {
        HttpExchange exchange = method.getDeclaringClass().getAnnotation(HttpExchange.class);
        if (exchange != null && MediaType.APPLICATION_FORM_URLENCODED.equals(exchange.contentType())) {
            return true;
        }
        PostExchange post = method.getAnnotation(PostExchange.class);
        if (post != null && MediaType.APPLICATION_FORM_URLENCODED.equals(post.contentType())) {
            return true;
        }
        PutExchange put = method.getAnnotation(PutExchange.class);
        return put != null && MediaType.APPLICATION_FORM_URLENCODED.equals(put.contentType());
    }

    private String encodeFormData(MultiValueMap<?, ?> formData) {
        StringJoiner joiner = new StringJoiner("&");
        formData.forEach((key, values) -> {
            for (Object value : values) {
                joiner.add(encode(String.valueOf(key)) + "=" + encode(String.valueOf(value)));
            }
        });
        return joiner.toString();
    }

    private String encodeMap(Map<?, ?> map) {
        StringJoiner joiner = new StringJoiner("&");
        map.forEach((key, value) ->
                joiner.add(encode(String.valueOf(key)) + "=" + encode(String.valueOf(value))));
        return joiner.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
