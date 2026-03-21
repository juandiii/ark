package xyz.juandiii.ark.interceptor;

import xyz.juandiii.ark.http.RawResponse;

@FunctionalInterface
public interface ResponseInterceptor {

    RawResponse intercept(RawResponse response);
}
