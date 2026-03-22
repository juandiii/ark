package xyz.juandiii.ark.mutiny.http;

import io.smallrye.mutiny.Uni;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.http.AbstractClientRequest;
import xyz.juandiii.ark.http.RawResponse;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

import java.util.List;

public final class MutinyClientRequest extends AbstractClientRequest<MutinyClientRequest> {

    private final MutinyHttpTransport transport;

    public MutinyClientRequest(String method, String baseUrl, String path,
                               MutinyHttpTransport transport, JsonSerializer serializer,
                               List<RequestInterceptor> requestInterceptors,
                               List<ResponseInterceptor> responseInterceptors) {
        super(method, baseUrl, path, serializer, requestInterceptors, responseInterceptors);
        this.transport = transport;
    }

    public MutinyResponseSpec retrieve() {
        String serializedBody = prepareBody();
        Uni<RawResponse> uni = transport.send(method, buildUri(), headers, serializedBody, timeout);
        for (ResponseInterceptor interceptor : responseInterceptors) {
            uni = uni.onItem().transform(interceptor::intercept);
        }
        return new MutinyResponseSpec(uni, serializer);
    }
}
