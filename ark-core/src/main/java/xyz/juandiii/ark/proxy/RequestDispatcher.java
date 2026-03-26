package xyz.juandiii.ark.proxy;

import xyz.juandiii.ark.interceptor.RequestContext;

/**
 * Abstracts HTTP request creation across execution models (sync, async, mutiny).
 *
 * @author Juan Diego Lopez V.
 */
@FunctionalInterface
public interface RequestDispatcher {

    RequestContext dispatch(String httpMethod, String path);
}
