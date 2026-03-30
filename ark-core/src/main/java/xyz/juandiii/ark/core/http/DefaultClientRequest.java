package xyz.juandiii.ark.core.http;

import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.interceptor.RequestInterceptor;
import xyz.juandiii.ark.core.interceptor.ResponseInterceptor;

import java.util.List;

/**
 * Default implementation of {@link ClientRequest}.
 *
 * @author Juan Diego Lopez V.
 */
public final class DefaultClientRequest extends AbstractClientRequest<DefaultClientRequest> implements ClientRequest {

    private final HttpTransport transport;

    public DefaultClientRequest(String method, String baseUrl, String path,
                                HttpTransport transport, JsonSerializer serializer,
                                List<RequestInterceptor> requestInterceptors,
                                List<ResponseInterceptor> responseInterceptors) {
        super(method, baseUrl, path, serializer, requestInterceptors, responseInterceptors);
        this.transport = transport;
    }

    @Override
    public ClientResponse retrieve() {
        applyInterceptors();
        SerializedBody body = prepareBody();
        RawResponse raw = body.isBinary()
                ? transport.sendBinary(method, buildUri(), headers, body.binary(), timeout)
                : transport.send(method, buildUri(), headers, body.text(), timeout);
        for (ResponseInterceptor interceptor : responseInterceptors) {
            raw = interceptor.intercept(raw);
        }
        validateResponse(raw);
        return new DefaultClientResponse(raw, serializer);
    }
}
