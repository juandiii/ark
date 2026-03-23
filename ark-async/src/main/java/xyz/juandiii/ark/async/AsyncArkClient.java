package xyz.juandiii.ark.async;

import xyz.juandiii.ark.AbstractArkClient;
import xyz.juandiii.ark.AbstractArkBuilder;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.async.http.DefaultAsyncClientRequest;
import xyz.juandiii.ark.async.http.AsyncHttpTransport;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

import java.util.List;
import java.util.Objects;

/**
 * Default async implementation of {@link AsyncArk}.
 *
 * @author Juan Diego Lopez V.
 */
public class AsyncArkClient extends AbstractArkClient<DefaultAsyncClientRequest> implements AsyncArk {

    private final AsyncHttpTransport transport;

    private AsyncArkClient(AsyncHttpTransport transport, JsonSerializer serializer,
                           String userAgent, String baseUrl,
                           List<RequestInterceptor> requestInterceptors,
                           List<ResponseInterceptor> responseInterceptors) {
        super(serializer, userAgent, baseUrl, requestInterceptors, responseInterceptors);
        this.transport = transport;
    }

    @Override
    protected DefaultAsyncClientRequest createRequest(String method, String path) {
        return new DefaultAsyncClientRequest(method, baseUrl, path, transport, serializer,
                requestInterceptors, responseInterceptors)
                .header("User-Agent", userAgent);
    }

    // ---- Builder ----

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractArkBuilder<Builder> {

        private AsyncHttpTransport transport;

        private Builder() {}

        public Builder transport(AsyncHttpTransport transport) {
            this.transport = transport;
            return this;
        }

        public AsyncArk build() {
            Objects.requireNonNull(serializer, "serializer must not be null");
            Objects.requireNonNull(transport, "transport must not be null");
            return new AsyncArkClient(transport, serializer, buildUserAgent(),
                    baseUrl, requestInterceptors, responseInterceptors);
        }
    }
}
