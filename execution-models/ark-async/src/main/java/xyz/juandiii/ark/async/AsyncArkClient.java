package xyz.juandiii.ark.async;

import xyz.juandiii.ark.async.http.DefaultAsyncClientRequest;
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
                           List<ResponseInterceptor> responseInterceptors,
                           boolean throwOnErrorDefault) {
        super(serializer, userAgent, baseUrl, requestInterceptors, responseInterceptors,
                throwOnErrorDefault);
        this.transport = transport;
    }

    @Override
    protected DefaultAsyncClientRequest createRequest(String method, String path) {
        DefaultAsyncClientRequest req = new DefaultAsyncClientRequest(method, baseUrl, path, transport, serializer,
                requestInterceptors, responseInterceptors);
        return req.throwOnError(throwOnErrorDefault)
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
         * Set the async transport. Required. Accepts any
         * {@code Transport<CompletableFuture<RawResponse>>} — typically an
         * {@code ArkJdkAsyncTransport} (optionally wrapped with the decorator
         * chain: {@code jdk.with(Retry.of(policy, new AsyncRetryOps()))}).
         *
         * @param transport configured transport instance
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
                    baseUrl, requestInterceptors, responseInterceptors,
                    throwOnErrorDefault);
        }
    }
}
