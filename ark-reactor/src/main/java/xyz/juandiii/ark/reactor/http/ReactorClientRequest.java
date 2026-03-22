package xyz.juandiii.ark.reactor.http;

import reactor.core.publisher.Mono;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.http.AbstractClientRequest;
import xyz.juandiii.ark.http.RawResponse;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

import java.util.List;

public final class ReactorClientRequest extends AbstractClientRequest<ReactorClientRequest> {

    private final ReactorHttpTransport transport;

    public ReactorClientRequest(String method, String baseUrl, String path,
                                ReactorHttpTransport transport, JsonSerializer serializer,
                                List<RequestInterceptor> requestInterceptors,
                                List<ResponseInterceptor> responseInterceptors) {
        super(method, baseUrl, path, serializer, requestInterceptors, responseInterceptors);
        this.transport = transport;
    }

    public ReactorResponseSpec retrieve() {
        String serializedBody = prepareBody();
        Mono<RawResponse> mono = transport.send(method, buildUri(), headers, serializedBody, timeout);
        for (ResponseInterceptor interceptor : responseInterceptors) {
            mono = mono.map(interceptor::intercept);
        }
        return new ReactorResponseSpec(mono, serializer);
    }
}
