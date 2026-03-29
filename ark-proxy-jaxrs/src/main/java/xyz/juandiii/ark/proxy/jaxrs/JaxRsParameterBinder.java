package xyz.juandiii.ark.proxy.jaxrs;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import xyz.juandiii.ark.core.interceptor.RequestContext;
import xyz.juandiii.ark.core.proxy.FormEncoder;
import xyz.juandiii.ark.core.proxy.ParameterBinder;
import xyz.juandiii.ark.core.type.MediaType;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Binds method parameters to HTTP request components using JAX-RS annotations.
 *
 * @author Juan Diego Lopez V.
 */
final class JaxRsParameterBinder implements ParameterBinder {

    @Override
    public void apply(RequestContext request, Method method, Object[] args) {
        if (args == null) return;
        Parameter[] params = method.getParameters();

        Map<String, String> formParams = new LinkedHashMap<>();
        Object implicitBody = null;

        for (int i = 0; i < params.length; i++) {
            if (params[i].isAnnotationPresent(PathParam.class)) continue;
            if (applyQueryParam(request, params[i], args[i])) continue;
            if (applyHeader(request, params[i], args[i])) continue;
            if (collectFormParam(params[i], args[i], formParams)) continue;
            if (isImplicitBodyParam(params[i])) {
                implicitBody = args[i];
            }
        }

        if (!formParams.isEmpty()) {
            request.contentType(MediaType.APPLICATION_FORM_URLENCODED);
            request.body(FormEncoder.encode(formParams));
        } else if (implicitBody != null) {
            request.body(implicitBody);
        }
    }

    private boolean applyQueryParam(RequestContext request, Parameter param, Object value) {
        QueryParam qp = param.getAnnotation(QueryParam.class);
        if (qp == null) return false;
        if (value != null) request.queryParam(qp.value(), String.valueOf(value));
        return true;
    }

    private boolean applyHeader(RequestContext request, Parameter param, Object value) {
        HeaderParam hp = param.getAnnotation(HeaderParam.class);
        if (hp == null) return false;
        if (value != null) request.header(hp.value(), String.valueOf(value));
        return true;
    }

    private boolean collectFormParam(Parameter param, Object value, Map<String, String> formParams) {
        FormParam fp = param.getAnnotation(FormParam.class);
        if (fp == null) return false;
        if (value != null) formParams.put(fp.value(), String.valueOf(value));
        return true;
    }

    private boolean isImplicitBodyParam(Parameter param) {
        return !param.isAnnotationPresent(PathParam.class)
                && !param.isAnnotationPresent(QueryParam.class)
                && !param.isAnnotationPresent(HeaderParam.class)
                && !param.isAnnotationPresent(FormParam.class);
    }
}