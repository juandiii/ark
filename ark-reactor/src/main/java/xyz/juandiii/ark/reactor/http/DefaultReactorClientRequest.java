package xyz.juandiii.ark.reactor.http;

import reactor.core.publisher.Mono;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.http.AbstractClientRequest;
import xyz.juandiii.ark.http.RawResponse;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

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
        String serializedBody = serializeBody();
        Mono<RawResponse> mono = transport.send(method, buildUri(), headers, serializedBody, timeout);
        for (ResponseInterceptor interceptor : responseInterceptors) {
            mono = mono.map(interceptor::intercept);
        }
        mono = mono.doOnNext(this::validateResponse);
        return new DefaultReactorClientResponse(mono, serializer);
    }
}
