package xyz.juandiii.ark.async.http;

import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.http.AbstractClientRequest;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.interceptor.RequestInterceptor;
import xyz.juandiii.ark.core.interceptor.ResponseInterceptor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of {@link AsyncClientRequest}.
 *
 * @author Juan Diego Lopez V.
 */
public final class DefaultAsyncClientRequest extends AbstractClientRequest<DefaultAsyncClientRequest> implements AsyncClientRequest {

    private final AsyncHttpTransport transport;

    public DefaultAsyncClientRequest(String method, String baseUrl, String path,
                                     AsyncHttpTransport transport, JsonSerializer serializer,
                                     List<RequestInterceptor> requestInterceptors,
                                     List<ResponseInterceptor> responseInterceptors) {
        super(method, baseUrl, path, serializer, requestInterceptors, responseInterceptors);
        this.transport = transport;
    }

    @Override
    public AsyncClientResponse retrieve() {
        applyInterceptors();
        String serializedBody = serializeBody();
        CompletableFuture<RawResponse> future = transport.sendAsync(
                method, buildUri(), headers, serializedBody, timeout);
        for (ResponseInterceptor interceptor : responseInterceptors) {
            future = future.thenApply(interceptor::intercept);
        }
        future = future.thenApply(raw -> { validateResponse(raw); return raw; });
        return new DefaultAsyncClientResponse(future, serializer);
    }
}
