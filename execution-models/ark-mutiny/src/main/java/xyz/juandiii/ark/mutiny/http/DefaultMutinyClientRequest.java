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
        SerializedBody body = prepareBody();
        Uni<RawResponse> uni = body.isBinary()
                ? transport.sendBinary(method, buildUri(), headers, body.binary(), timeout)
                : transport.send(method, buildUri(), headers, body.text(), timeout);
        for (ResponseInterceptor interceptor : responseInterceptors) {
            uni = uni.onItem().transform(interceptor::intercept);
        }
        uni = uni.onItem().invoke(this::validateResponse);
        return new DefaultMutinyClientResponse(uni, serializer);
    }
}
