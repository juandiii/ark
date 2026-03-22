package xyz.juandiii.ark.http;

import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

import java.util.List;

public final class ClientRequest extends AbstractClientRequest<ClientRequest> {

    private final HttpTransport transport;

    public ClientRequest(String method, String baseUrl, String path,
                         HttpTransport transport, JsonSerializer serializer,
                         List<RequestInterceptor> requestInterceptors,
                         List<ResponseInterceptor> responseInterceptors) {
        super(method, baseUrl, path, serializer, requestInterceptors, responseInterceptors);
        this.transport = transport;
    }

    public ResponseSpec retrieve() {
        String serializedBody = prepareBody();
        RawResponse raw = transport.send(method, buildUri(), headers, serializedBody, timeout);
        for (ResponseInterceptor interceptor : responseInterceptors) {
            raw = interceptor.intercept(raw);
        }
        return new ResponseSpec(raw, serializer);
    }
}
