package xyz.juandiii.ark.vertx;

import xyz.juandiii.ark.core.AbstractArkClient;
import xyz.juandiii.ark.core.AbstractArkBuilder;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.vertx.http.DefaultVertxClientRequest;
import io.vertx.core.Future;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.Transport;
import xyz.juandiii.ark.core.interceptor.RequestInterceptor;
import xyz.juandiii.ark.core.interceptor.ResponseInterceptor;

import java.util.List;
import java.util.Objects;

/**
 * Default Vert.x implementation of {@link VertxArk}.
 *
 * @author Juan Diego Lopez V.
 */
public class VertxArkClient extends AbstractArkClient<DefaultVertxClientRequest> implements VertxArk {

    private final Transport<Future<RawResponse>> transport;

    private VertxArkClient(Transport<Future<RawResponse>> transport, JsonSerializer serializer,
                           String userAgent, String baseUrl,
                           List<RequestInterceptor> requestInterceptors,
                           List<ResponseInterceptor> responseInterceptors,
                           boolean throwOnErrorDefault) {
        super(serializer, userAgent, baseUrl, requestInterceptors, responseInterceptors,
                throwOnErrorDefault);
        this.transport = transport;
    }

    @Override
    protected DefaultVertxClientRequest createRequest(String method, String path) {
        DefaultVertxClientRequest req = new DefaultVertxClientRequest(method, baseUrl, path, transport, serializer,
                requestInterceptors, responseInterceptors);
        return req.throwOnError(throwOnErrorDefault)
                .header("User-Agent", userAgent);
    }

    // ---- Builder ----

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractArkBuilder<Builder> {

        private Transport<Future<RawResponse>> transport;

        private Builder() {}

        /**
         * Set the HTTP transport. Required.
         *
         * <p>Accepts any {@code Transport<Future<RawResponse>>} — a bare
         * {@link xyz.juandiii.ark.vertx.http.VertxHttpTransport} or a decorator-chain result such as
         * {@code transport.with(Retry.of(policy, new VertxRetryOps()))}.
         *
         * @param transport configured transport instance
         * @return this builder for chaining
         */
        public Builder transport(Transport<Future<RawResponse>> transport) {
            this.transport = transport;
            return this;
        }

        public VertxArk build() {
            if (serializer == null) {
                serializer = new VertxJsonSerializer();
            }
            Objects.requireNonNull(transport, "transport must not be null");
            logConfiguration("VertxArkClient (Future)", transport.getClass().getSimpleName());
            return new VertxArkClient(transport, serializer, buildUserAgent(),
                    baseUrl, requestInterceptors, responseInterceptors,
                    throwOnErrorDefault);
        }
    }
}
