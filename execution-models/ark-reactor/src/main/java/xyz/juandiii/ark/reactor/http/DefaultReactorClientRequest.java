package xyz.juandiii.ark.reactor.http;

import reactor.core.publisher.Mono;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.http.AbstractClientRequest;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.interceptor.RequestInterceptor;
import xyz.juandiii.ark.core.interceptor.ResponseInterceptor;

import java.util.List;

/**
 * Default implementation of {@link ReactorClientRequest}.
 *
 * @author Juan Diego Lopez V.
 */
public final class DefaultReactorClientRequest extends AbstractClientRequest<DefaultReactorClientRequest>
        implements ReactorClientRequest {

    private final ReactorHttpTransport transport;

    public DefaultReactorClientRequest(String method, String baseUrl, String path,
                                ReactorHttpTransport transport, JsonSerializer serializer,
                                List<RequestInterceptor> requestInterceptors,
                                List<ResponseInterceptor> responseInterceptors) {
        super(method, baseUrl, path, serializer, requestInterceptors, responseInterceptors);
        this.transport = transport;
    }

    @Override
    public ReactorClientResponse retrieve() {
        applyInterceptors();
        SerializedBody body = prepareBody();
        Mono<RawResponse> mono = body.isBinary()
                ? transport.sendBinary(method, buildUri(), headers, body.binary(), timeout)
                : transport.send(method, buildUri(), headers, body.text(), timeout);
        for (ResponseInterceptor interceptor : responseInterceptors) {
            mono = mono.map(interceptor::intercept);
        }
        mono = mono.doOnNext(this::validateResponse);
        return new DefaultReactorClientResponse(mono, serializer);
    }
}
