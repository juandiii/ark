package xyz.juandiii.ark.async.http;

import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.http.AbstractClientRequest;
import xyz.juandiii.ark.http.RawResponse;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class AsyncClientRequest extends AbstractClientRequest<AsyncClientRequest> {

    private final AsyncHttpTransport transport;

    public AsyncClientRequest(String method, String baseUrl, String path,
                              AsyncHttpTransport transport, JsonSerializer serializer,
                              List<RequestInterceptor> requestInterceptors,
                              List<ResponseInterceptor> responseInterceptors) {
        super(method, baseUrl, path, serializer, requestInterceptors, responseInterceptors);
        this.transport = transport;
    }

    public AsyncResponseSpec retrieve() {
        String serializedBody = prepareBody();
        CompletableFuture<RawResponse> future = transport.sendAsync(
                method, buildUri(), headers, serializedBody, timeout);
        for (ResponseInterceptor interceptor : responseInterceptors) {
            future = future.thenApply(interceptor::intercept);
        }
        return new AsyncResponseSpec(future, serializer);
    }
}
