package xyz.juandiii.ark.core.interceptor;

import xyz.juandiii.ark.core.http.RawResponse;

/**
 * Functional interface for post-response interception.
 *
 * @author Juan Diego Lopez V.
 */
@FunctionalInterface
public interface ResponseInterceptor {

    RawResponse intercept(RawResponse response);
}
