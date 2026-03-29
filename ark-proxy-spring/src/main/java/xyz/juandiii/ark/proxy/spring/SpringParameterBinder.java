package xyz.juandiii.ark.proxy.spring;

import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import xyz.juandiii.ark.core.interceptor.RequestContext;
import xyz.juandiii.ark.core.proxy.FormEncoder;
import xyz.juandiii.ark.core.proxy.ParameterBinder;
import xyz.juandiii.ark.core.type.MediaType;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.StringJoiner;

/**
 * Binds method parameters to HTTP request components using Spring annotations.
 *
 * @author Juan Diego Lopez V.
 */
final class SpringParameterBinder implements ParameterBinder {

    @Override
    public void apply(RequestContext request, Method method, Object[] args) {
        if (args == null) return;
        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
            if (params[i].isAnnotationPresent(PathVariable.class)) {
                continue;
            }
            if (applyQueryParam(request, params[i], args[i])) continue;
            if (applyHeader(request, params[i], args[i])) continue;
            if (params[i].isAnnotationPresent(RequestBody.class) || isImplicitBodyParam(params[i])) {
                applyBody(request, args[i]);
            }
        }
    }

    private boolean applyQueryParam(RequestContext request, Parameter param, Object value) {
        RequestParam rp = param.getAnnotation(RequestParam.class);
        if (rp == null) return false;
        String name = rp.value().isEmpty() ? param.getName() : rp.value();
        if (value != null) request.queryParam(name, String.valueOf(value));
        return true;
    }

    private boolean applyHeader(RequestContext request, Parameter param, Object value) {
        RequestHeader rh = param.getAnnotation(RequestHeader.class);
        if (rh == null) return false;
        String name = rh.value().isEmpty() ? param.getName() : rh.value();
        if (value != null) request.header(name, String.valueOf(value));
        return true;
    }

    private void applyBody(RequestContext request, Object body) {
        if (body instanceof MultiValueMap<?, ?> formData) {
            request.contentType(MediaType.APPLICATION_FORM_URLENCODED);
            request.body(encodeMultiValueMap(formData));
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

    private String encodeMultiValueMap(MultiValueMap<?, ?> formData) {
        StringJoiner joiner = new StringJoiner("&");
        formData.forEach((key, values) -> {
            for (Object value : values) {
                joiner.add(FormEncoder.urlEncode(String.valueOf(key)) + "=" + FormEncoder.urlEncode(String.valueOf(value)));
            }
        });
        return joiner.toString();
    }
}
