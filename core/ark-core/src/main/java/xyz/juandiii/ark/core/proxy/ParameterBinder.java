package xyz.juandiii.ark.core.proxy;

import xyz.juandiii.ark.core.interceptor.RequestContext;

import java.lang.reflect.Method;

/**
 * Binds method parameters to HTTP request components.
 *
 * @author Juan Diego Lopez V.
 */
public interface ParameterBinder {

    void apply(RequestContext request, Method method, Object[] args);
}
