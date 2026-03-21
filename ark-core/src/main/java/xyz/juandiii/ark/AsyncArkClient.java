package xyz.juandiii.ark;

import xyz.juandiii.ark.http.AsyncHttpTransport;
import xyz.juandiii.ark.http.AsyncClientRequest;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

import java.util.List;

final class AsyncArkClient implements AsyncArk {

    private final AsyncHttpTransport transport;
    private final JsonSerializer serializer;
    private final String userAgent;
    private final String baseUrl;
    private final List<RequestInterceptor> requestInterceptors;
    private final List<ResponseInterceptor> responseInterceptors;

    AsyncArkClient(AsyncHttpTransport transport, JsonSerializer serializer, String userAgent,
                   String baseUrl,
                   List<RequestInterceptor> requestInterceptors,
                   List<ResponseInterceptor> responseInterceptors) {
        this.transport = transport;
        this.serializer = serializer;
        this.userAgent = userAgent;
        this.baseUrl = baseUrl;
        this.requestInterceptors = List.copyOf(requestInterceptors);
        this.responseInterceptors = List.copyOf(responseInterceptors);
    }

    @Override
    public AsyncClientRequest get(String path) {
        return spec("GET", path);
    }

    @Override
    public AsyncClientRequest post(String path) {
        return spec("POST", path);
    }

    @Override
    public AsyncClientRequest put(String path) {
        return spec("PUT", path);
    }

    @Override
    public AsyncClientRequest patch(String path) {
        return spec("PATCH", path);
    }

    @Override
    public AsyncClientRequest delete(String path) {
        return spec("DELETE", path);
    }

    private AsyncClientRequest spec(String method, String path) {
        return new AsyncClientRequest(method, baseUrl, path, transport, serializer,
                requestInterceptors, responseInterceptors)
                .header("User-Agent", userAgent);
    }
}