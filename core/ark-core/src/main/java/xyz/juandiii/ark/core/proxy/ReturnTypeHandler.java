package xyz.juandiii.ark.core.proxy;

import xyz.juandiii.ark.core.interceptor.RequestContext;

import java.lang.reflect.Type;

/**
 * Dispatches request execution based on method return type.
 * Implementations cast the RequestContext to the appropriate request type internally.
 *
 * @author Juan Diego Lopez V.
 */
public interface ReturnTypeHandler {

    Object handle(RequestContext request, Type returnType);
}