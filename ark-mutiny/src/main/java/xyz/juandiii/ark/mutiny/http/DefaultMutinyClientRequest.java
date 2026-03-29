package xyz.juandiii.ark.mutiny.http;

import io.smallrye.mutiny.Uni;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.http.AbstractClientRequest;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.interceptor.RequestInterceptor;
import xyz.juandiii.ark.core.interceptor.ResponseInterceptor;

import java.util.List;

/**
 * Default implementation of {@link MutinyClientRequest}.
 *
 * @author Juan Diego Lopez V.
 */
public final class DefaultMutinyClientRequest extends AbstractClientRequest<DefaultMutinyClientRequest>
        implements MutinyClientRequest {

    private final MutinyHttpTransport transport;

    public DefaultMutinyClientRequest(String method, String baseUrl, String path,
                               MutinyHttpTransport transport, JsonSerializer serializer,
                               List<RequestInterceptor> requestInterceptors,
                               List<ResponseInterceptor> responseInterceptors) {
        super(method, baseUrl, path, serializer, requestInterceptors, responseInterceptors);
        this.transport = transport;
    }

    @Override
    public MutinyClientResponse retrieve() {
        applyInterceptors();
        String serializedBody = serializeBody();
        Uni<RawResponse> uni = transport.send(method, buildUri(), headers, serializedBody, timeout);
        for (ResponseInterceptor interceptor : responseInterceptors) {
            uni = uni.onItem().transform(interceptor::intercept);
        }
        uni = uni.onItem().invoke(this::validateResponse);
        return new DefaultMutinyClientResponse(uni, serializer);
    }
}
