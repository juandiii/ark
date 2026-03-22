package xyz.juandiii.ark.vertx;

import xyz.juandiii.ark.AbstractArkClient;
import xyz.juandiii.ark.ArkClient;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.vertx.http.VertxClientRequest;
import xyz.juandiii.ark.vertx.http.VertxHttpTransport;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

import java.util.List;
import java.util.Objects;

public class VertxArkClient extends AbstractArkClient<VertxClientRequest> implements VertxArk {

    private final VertxHttpTransport transport;

    private VertxArkClient(VertxHttpTransport transport, JsonSerializer serializer,
                           String userAgent, String baseUrl,
                           List<RequestInterceptor> requestInterceptors,
                           List<ResponseInterceptor> responseInterceptors) {
        super(serializer, userAgent, baseUrl, requestInterceptors, responseInterceptors);
        this.transport = transport;
    }

    @Override
    protected VertxClientRequest createRequest(String method, String path) {
        return new VertxClientRequest(method, baseUrl, path, transport, serializer,
                requestInterceptors, responseInterceptors)
                .header("User-Agent", userAgent);
    }

    // ---- Builder ----

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends ArkClient.AbstractBuilder<Builder> {

        private VertxHttpTransport transport;

        private Builder() {}

        public Builder transport(VertxHttpTransport transport) {
            this.transport = transport;
            return this;
        }

        public VertxArk build() {
            Objects.requireNonNull(serializer, "serializer must not be null");
            Objects.requireNonNull(transport, "transport must not be null");
            return new VertxArkClient(transport, serializer, buildUserAgent(),
                    baseUrl, requestInterceptors, responseInterceptors);
        }
    }
}
