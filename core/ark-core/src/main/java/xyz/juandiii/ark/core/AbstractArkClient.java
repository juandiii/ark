package xyz.juandiii.ark.core;

import xyz.juandiii.ark.core.http.AbstractClientRequest;
import xyz.juandiii.ark.core.interceptor.RequestInterceptor;
import xyz.juandiii.ark.core.interceptor.ResponseInterceptor;

import java.util.List;

/**
 * Template Method base for all Ark client implementations.
 *
 * @author Juan Diego Lopez V.
 */
public abstract class AbstractArkClient<R extends AbstractClientRequest<R>> {

    protected final JsonSerializer serializer;
    protected final String userAgent;
    protected final String baseUrl;
    protected final List<RequestInterceptor> requestInterceptors;
    protected final List<ResponseInterceptor> responseInterceptors;

    protected AbstractArkClient(JsonSerializer serializer, String userAgent, String baseUrl,
                                List<RequestInterceptor> requestInterceptors,
                                List<ResponseInterceptor> responseInterceptors) {
        this.serializer = serializer;
        this.userAgent = userAgent;
        this.baseUrl = baseUrl;
        this.requestInterceptors = List.copyOf(requestInterceptors);
        this.responseInterceptors = List.copyOf(responseInterceptors);
    }

    protected abstract R createRequest(String method, String path);

    public R get(String path) {
        return createRequest("GET", path);
    }

    public R post(String path) {
        return createRequest("POST", path);
    }

    public R put(String path) {
        return createRequest("PUT", path);
    }

    public R patch(String path) {
        return createRequest("PATCH", path);
    }

    public R delete(String path) {
        return createRequest("DELETE", path);
    }
}
