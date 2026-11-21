package xyz.juandiii.ark.mutiny;

import xyz.juandiii.ark.core.AbstractArkClient;
import xyz.juandiii.ark.core.AbstractArkBuilder;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.mutiny.http.DefaultMutinyClientRequest;
import io.smallrye.mutiny.Uni;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.Transport;
import xyz.juandiii.ark.core.interceptor.RequestInterceptor;
import xyz.juandiii.ark.core.interceptor.ResponseInterceptor;

import java.util.List;
import java.util.Objects;

/**
 * Default Mutiny implementation of {@link MutinyArk}.
 *
 * @author Juan Diego Lopez V.
 */
public class MutinyArkClient extends AbstractArkClient<DefaultMutinyClientRequest> implements MutinyArk {

    private final Transport<Uni<RawResponse>> transport;

    private MutinyArkClient(Transport<Uni<RawResponse>> transport, JsonSerializer serializer,
                            String userAgent, String baseUrl,
                            List<RequestInterceptor> requestInterceptors,
                            List<ResponseInterceptor> responseInterceptors,
                            boolean throwOnErrorDefault) {
        super(serializer, userAgent, baseUrl, requestInterceptors, responseInterceptors,
                throwOnErrorDefault);
        this.transport = transport;
    }

    @Override
    protected DefaultMutinyClientRequest createRequest(String method, String path) {
        DefaultMutinyClientRequest req = new DefaultMutinyClientRequest(method, baseUrl, path, transport, serializer,
                requestInterceptors, responseInterceptors);
        return req.throwOnError(throwOnErrorDefault)
                .header("User-Agent", userAgent);
    }

    // ---- Builder ----

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractArkBuilder<Builder> {

        private Transport<Uni<RawResponse>> transport;

        private Builder() {}

        /**
         * Set the HTTP transport. Required.
         *
         * <p>Accepts any {@code Transport<Uni<RawResponse>>} — a bare {@link xyz.juandiii.ark.mutiny.http.MutinyHttpTransport}
         * or a decorator-chain result such as
         * {@code transport.with(Retry.of(policy, new MutinyRetryOps()))}.
         *
         * @param transport configured transport instance
         * @return this builder for chaining
         */
        public Builder transport(Transport<Uni<RawResponse>> transport) {
            this.transport = transport;
            return this;
        }

        public MutinyArk build() {
            Objects.requireNonNull(serializer, "serializer must not be null");
            Objects.requireNonNull(transport, "transport must not be null");
            logConfiguration("MutinyArkClient (Uni/Multi)", transport.getClass().getSimpleName());
            return new MutinyArkClient(transport, serializer, buildUserAgent(),
                    baseUrl, requestInterceptors, responseInterceptors,
                    throwOnErrorDefault);
        }
    }
}
