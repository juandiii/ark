package xyz.juandiii.ark.reactor;

import xyz.juandiii.ark.AbstractArkClient;
import xyz.juandiii.ark.ArkClient;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.reactor.http.ReactorClientRequest;
import xyz.juandiii.ark.reactor.http.ReactorHttpTransport;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

import java.util.List;
import java.util.Objects;

public class ReactorArkClient extends AbstractArkClient<ReactorClientRequest> implements ReactorArk {

    private final ReactorHttpTransport transport;

    private ReactorArkClient(ReactorHttpTransport transport, JsonSerializer serializer,
                             String userAgent, String baseUrl,
                             List<RequestInterceptor> requestInterceptors,
                             List<ResponseInterceptor> responseInterceptors) {
        super(serializer, userAgent, baseUrl, requestInterceptors, responseInterceptors);
        this.transport = transport;
    }

    @Override
    protected ReactorClientRequest createRequest(String method, String path) {
        return new ReactorClientRequest(method, baseUrl, path, transport, serializer,
                requestInterceptors, responseInterceptors)
                .header("User-Agent", userAgent);
    }

    // ---- Builder ----

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends ArkClient.AbstractBuilder<Builder> {

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
