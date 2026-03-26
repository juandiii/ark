package xyz.juandiii.ark.vertx;

import xyz.juandiii.ark.AbstractArkClient;
import xyz.juandiii.ark.AbstractArkBuilder;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.vertx.http.DefaultVertxClientRequest;
import xyz.juandiii.ark.vertx.http.VertxHttpTransport;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

import java.util.List;
import java.util.Objects;

/**
 * Default Vert.x implementation of {@link VertxArk}.
 *
 * @author Juan Diego Lopez V.
 */
public class VertxArkClient extends AbstractArkClient<DefaultVertxClientRequest> implements VertxArk {

    private final VertxHttpTransport transport;

    private VertxArkClient(VertxHttpTransport transport, JsonSerializer serializer,
                           String userAgent, String baseUrl,
                           List<RequestInterceptor> requestInterceptors,
                           List<ResponseInterceptor> responseInterceptors) {
        super(serializer, userAgent, baseUrl, requestInterceptors, responseInterceptors);
        this.transport = transport;
    }

    @Override
    protected DefaultVertxClientRequest createRequest(String method, String path) {
        return new DefaultVertxClientRequest(method, baseUrl, path, transport, serializer,
                requestInterceptors, responseInterceptors)
                .header("User-Agent", userAgent);
    }

    // ---- Builder ----

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractArkBuilder<Builder> {

        private VertxHttpTransport transport;

        private Builder() {}

        public Builder transport(VertxHttpTransport transport) {
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
                    baseUrl, requestInterceptors, responseInterceptors);
        }
    }
}
