package xyz.juandiii.ark.reactor;

import xyz.juandiii.ark.core.AbstractArkClient;
import xyz.juandiii.ark.core.AbstractArkBuilder;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.reactor.http.DefaultReactorClientRequest;
import reactor.core.publisher.Mono;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.Transport;
import xyz.juandiii.ark.core.interceptor.RequestInterceptor;
import xyz.juandiii.ark.core.interceptor.ResponseInterceptor;

import java.util.List;
import java.util.Objects;

/**
 * Default reactive implementation of {@link ReactorArk}.
 *
 * @author Juan Diego Lopez V.
 */
public class ReactorArkClient extends AbstractArkClient<DefaultReactorClientRequest> implements ReactorArk {

    private final Transport<Mono<RawResponse>> transport;

    private ReactorArkClient(Transport<Mono<RawResponse>> transport, JsonSerializer serializer,
                             String userAgent, String baseUrl,
                             List<RequestInterceptor> requestInterceptors,
                             List<ResponseInterceptor> responseInterceptors,
                             boolean throwOnErrorDefault) {
        super(serializer, userAgent, baseUrl, requestInterceptors, responseInterceptors,
                throwOnErrorDefault);
        this.transport = transport;
    }

    @Override
    protected DefaultReactorClientRequest createRequest(String method, String path) {
        DefaultReactorClientRequest req = new DefaultReactorClientRequest(method, baseUrl, path, transport, serializer,
                requestInterceptors, responseInterceptors);
        return req.throwOnError(throwOnErrorDefault)
                .header("User-Agent", userAgent);
    }

    // ---- Builder ----

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractArkBuilder<Builder> {

        private Transport<Mono<RawResponse>> transport;

        private Builder() {}

        /**
         * Set the HTTP transport. Required.
         *
         * <p>Accepts any {@code Transport<Mono<RawResponse>>} — a bare {@link xyz.juandiii.ark.reactor.http.ReactorHttpTransport}
         * or a decorator-chain result such as
         * {@code transport.with(Retry.of(policy, new ReactorRetryOps()))}.
         *
         * @param transport configured transport instance
         * @return this builder for chaining
         */
        public Builder transport(Transport<Mono<RawResponse>> transport) {
            this.transport = transport;
            return this;
        }

        public ReactorArk build() {
            Objects.requireNonNull(serializer, "serializer must not be null");
            Objects.requireNonNull(transport, "transport must not be null");
            logConfiguration("ReactorArkClient (Mono/Flux)", transport.getClass().getSimpleName());
            return new ReactorArkClient(transport, serializer, buildUserAgent(),
                    baseUrl, requestInterceptors, responseInterceptors,
                    throwOnErrorDefault);
        }
    }
}
