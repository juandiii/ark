package xyz.juandiii.ark.async;

import xyz.juandiii.ark.async.http.AsyncHttpTransport;
import xyz.juandiii.ark.async.http.DefaultAsyncClientRequest;
import xyz.juandiii.ark.async.http.decorator.Adapters;
import xyz.juandiii.ark.core.AbstractArkBuilder;
import xyz.juandiii.ark.core.AbstractArkClient;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.Transport;
import xyz.juandiii.ark.core.interceptor.RequestInterceptor;
import xyz.juandiii.ark.core.interceptor.ResponseInterceptor;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Default async implementation of {@link AsyncArk}.
 *
 * @author Juan Diego Lopez V.
 */
public class AsyncArkClient extends AbstractArkClient<DefaultAsyncClientRequest> implements AsyncArk {

    private final Transport<CompletableFuture<RawResponse>> transport;

    private AsyncArkClient(Transport<CompletableFuture<RawResponse>> transport, JsonSerializer serializer,
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

        private Transport<CompletableFuture<RawResponse>> transport;

        private Builder() {}

        /**
         * Set an {@link AsyncHttpTransport} (legacy / typical usage). Wraps it
         * via {@link Adapters#fromAsync(AsyncHttpTransport)} internally so the
         * client stores the unified Transport type.
         *
         * @param transport AsyncHttpTransport implementation
         * @return this builder for chaining
         */
        public Builder transport(AsyncHttpTransport transport) {
            this.transport = Adapters.fromAsync(transport);
            return this;
        }

        /**
         * Set a {@code Transport<CompletableFuture<RawResponse>>} — typically the
         * result of a decorator chain like
         * {@code Adapters.fromAsync(jdk).with(Retry.of(policy, new AsyncRetryOps()))}.
         *
         * @param transport unified transport instance
         * @return this builder for chaining
         */
        public Builder transport(Transport<CompletableFuture<RawResponse>> transport) {
            this.transport = transport;
            return this;
        }

        public AsyncArk build() {
            Objects.requireNonNull(serializer, "serializer must not be null");
            Objects.requireNonNull(transport, "transport must not be null");
            logConfiguration("AsyncArkClient (CompletableFuture)", transport.getClass().getSimpleName());
            return new AsyncArkClient(transport, serializer, buildUserAgent(),
                    baseUrl, requestInterceptors, responseInterceptors);
        }
    }
}
