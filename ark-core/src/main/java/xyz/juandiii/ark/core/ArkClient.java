package xyz.juandiii.ark.core;

import xyz.juandiii.ark.core.http.HttpTransport;
import xyz.juandiii.ark.core.http.DefaultClientRequest;
import xyz.juandiii.ark.core.interceptor.RequestInterceptor;
import xyz.juandiii.ark.core.interceptor.ResponseInterceptor;

import java.util.List;
import java.util.Objects;

/**
 * Default sync implementation of {@link Ark}, built via {@link #builder()}.
 * Holds the configured {@link HttpTransport}, {@link JsonSerializer}, base URL,
 * user-agent and interceptors, and produces a fresh {@link DefaultClientRequest}
 * per HTTP call.
 *
 * <p>Instances are thread-safe — the per-request state lives on the
 * {@link DefaultClientRequest} returned by each verb method.
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

    /**
     * @return a fresh {@link Builder} to configure a sync client
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ArkClient}. Inherits {@code serializer}, {@code baseUrl},
     * {@code userAgent}, {@code httpVersion}, timeouts and interceptors from
     * {@link AbstractArkBuilder}. Adds the required {@link #transport(HttpTransport)}.
     */
    public static final class Builder extends AbstractArkBuilder<Builder> {

        private HttpTransport transport;

        private Builder() {}

        /**
         * Set the synchronous HTTP transport. Required.
         *
         * @param transport configured transport instance
         * @return this builder for chaining
         */
        public Builder transport(HttpTransport transport) {
            this.transport = transport;
            return this;
        }

        /**
         * Build the configured client.
         *
         * @return immutable {@link Ark} instance
         * @throws NullPointerException if {@code serializer} or {@code transport} was not set
         */
        public Ark build() {
            Objects.requireNonNull(serializer, "serializer must not be null");
            Objects.requireNonNull(transport, "transport must not be null");
            logConfiguration("ArkClient (sync)", transport.getClass().getSimpleName());
            return new ArkClient(transport, serializer, buildUserAgent(),
                    baseUrl, requestInterceptors, responseInterceptors);
        }
    }
}
