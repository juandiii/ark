package xyz.juandiii.ark.core.interceptor;

/**
 * Functional interface for pre-request interception.
 *
 * @author Juan Diego Lopez V.
 */
public interface RequestInterceptor {

    void intercept(RequestContext request);
}
