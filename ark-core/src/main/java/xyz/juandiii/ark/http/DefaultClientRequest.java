package xyz.juandiii.ark.http;

import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

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
        String serializedBody = serializeBody();
        RawResponse raw = transport.send(method, buildUri(), headers, serializedBody, timeout);
        for (ResponseInterceptor interceptor : responseInterceptors) {
            raw = interceptor.intercept(raw);
        }
        validateResponse(raw);
        return new DefaultClientResponse(raw, serializer);
    }
}
