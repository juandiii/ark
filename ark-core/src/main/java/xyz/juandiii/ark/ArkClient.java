package xyz.juandiii.ark;

import xyz.juandiii.ark.http.HttpTransport;
import xyz.juandiii.ark.http.DefaultClientRequest;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

import java.util.List;
import java.util.Objects;

/**
 * Default sync implementation of {@link Ark} with builder pattern.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkClient extends AbstractArkClient<DefaultClientRequest> implements Ark {

    private final HttpTransport transport;

    private ArkClient(HttpTransport transport, JsonSerializer serializer, String userAgent,
                      String baseUrl,
                      List<RequestInterceptor> requestInterceptors,
                      List<ResponseInterceptor> responseInterceptors) {
        super(serializer, userAgent, baseUrl, requestInterceptors, responseInterceptors);
        this.transport = transport;
    }

    @Override
    protected DefaultClientRequest createRequest(String method, String path) {
        return new DefaultClientRequest(method, baseUrl, path, transport, serializer,
                requestInterceptors, responseInterceptors)
                .header("User-Agent", userAgent);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractArkBuilder<Builder> {

        private HttpTransport transport;

        private Builder() {}

        public Builder transport(HttpTransport transport) {
            this.transport = transport;
            return this;
        }

        public Ark build() {
            Objects.requireNonNull(serializer, "serializer must not be null");
            Objects.requireNonNull(transport, "transport must not be null");
            logConfiguration("ArkClient (sync)", transport.getClass().getSimpleName());
            return new ArkClient(transport, serializer, buildUserAgent(),
                    baseUrl, requestInterceptors, responseInterceptors);
        }
    }
}
