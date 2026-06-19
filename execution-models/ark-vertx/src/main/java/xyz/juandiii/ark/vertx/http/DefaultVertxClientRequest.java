package xyz.juandiii.ark.vertx.http;

import io.vertx.core.Future;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.http.AbstractClientRequest;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.interceptor.RequestInterceptor;
import xyz.juandiii.ark.core.interceptor.ResponseInterceptor;

import java.util.List;

/**
 * Default implementation of {@link VertxClientRequest}.
 *
 * @author Juan Diego Lopez V.
 */
public final class DefaultVertxClientRequest extends AbstractClientRequest<DefaultVertxClientRequest>
        implements VertxClientRequest {

    private final VertxHttpTransport transport;

    public DefaultVertxClientRequest(String method, String baseUrl, String path,
                              VertxHttpTransport transport, JsonSerializer serializer,
                              List<RequestInterceptor> requestInterceptors,
                              List<ResponseInterceptor> responseInterceptors) {
        super(method, baseUrl, path, serializer, requestInterceptors, responseInterceptors);
        this.transport = transport;
    }

    @Override
    public VertxClientResponse retrieve() {
        applyInterceptors();
        SerializedBody body = prepareBody();
        Future<RawResponse> future = body.isBinary()
                ? transport.sendBinary(method, buildUri(), headers, body.binary(), timeout)
                : transport.send(method, buildUri(), headers, body.text(), timeout);
        for (ResponseInterceptor interceptor : responseInterceptors) {
            future = future.map(interceptor::intercept);
        }
        future = future.map(raw -> { validateResponse(raw); return raw; });
        return new DefaultVertxClientResponse(future, serializer);
    }
}
