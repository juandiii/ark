package xyz.juandiii.ark.interceptor;

import xyz.juandiii.ark.http.RawResponse;

/**
 * Functional interface for post-response interception.
 *
 * @author Juan Diego Lopez V.
 */
@FunctionalInterface
public interface ResponseInterceptor {

    RawResponse intercept(RawResponse response);
}
