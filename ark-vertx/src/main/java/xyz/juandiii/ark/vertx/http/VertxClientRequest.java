package xyz.juandiii.ark.vertx.http;

import io.vertx.core.Future;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.http.AbstractClientRequest;
import xyz.juandiii.ark.http.RawResponse;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

import java.util.List;

public final class VertxClientRequest extends AbstractClientRequest<VertxClientRequest> {

    private final VertxHttpTransport transport;

    public VertxClientRequest(String method, String baseUrl, String path,
                              VertxHttpTransport transport, JsonSerializer serializer,
                              List<RequestInterceptor> requestInterceptors,
                              List<ResponseInterceptor> responseInterceptors) {
        super(method, baseUrl, path, serializer, requestInterceptors, responseInterceptors);
        this.transport = transport;
    }

    public VertxResponseSpec retrieve() {
        String serializedBody = prepareBody();
        Future<RawResponse> future = transport.send(method, buildUri(), headers, serializedBody, timeout);
        for (ResponseInterceptor interceptor : responseInterceptors) {
            future = future.map(interceptor::intercept);
        }
        return new VertxResponseSpec(future, serializer);
    }
}
