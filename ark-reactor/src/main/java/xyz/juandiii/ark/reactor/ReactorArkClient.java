package xyz.juandiii.ark.reactor;

import xyz.juandiii.ark.AbstractArkClient;
import xyz.juandiii.ark.AbstractArkBuilder;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.reactor.http.DefaultReactorClientRequest;
import xyz.juandiii.ark.reactor.http.ReactorHttpTransport;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

import java.util.List;
import java.util.Objects;

/**
 * Default reactive implementation of {@link ReactorArk}.
 *
 * @author Juan Diego Lopez V.
 */
public class ReactorArkClient extends AbstractArkClient<DefaultReactorClientRequest> implements ReactorArk {

    private final ReactorHttpTransport transport;

    private ReactorArkClient(ReactorHttpTransport transport, JsonSerializer serializer,
                             String userAgent, String baseUrl,
                             List<RequestInterceptor> requestInterceptors,
                             List<ResponseInterceptor> responseInterceptors) {
        super(serializer, userAgent, baseUrl, requestInterceptors, responseInterceptors);
        this.transport = transport;
    }

    @Override
    protected DefaultReactorClientRequest createRequest(String method, String path) {
        return new DefaultReactorClientRequest(method, baseUrl, path, transport, serializer,
                requestInterceptors, responseInterceptors)
                .header("User-Agent", userAgent);
    }

    // ---- Builder ----

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractArkBuilder<Builder> {

        private ReactorHttpTransport transport;

        private Builder() {}

        public Builder transport(ReactorHttpTransport transport) {
            this.transport = transport;
            return this;
        }

        public ReactorArk build() {
            Objects.requireNonNull(serializer, "serializer must not be null");
            Objects.requireNonNull(transport, "transport must not be null");
            return new ReactorArkClient(transport, serializer, buildUserAgent(),
                    baseUrl, requestInterceptors, responseInterceptors);
        }
    }
}
