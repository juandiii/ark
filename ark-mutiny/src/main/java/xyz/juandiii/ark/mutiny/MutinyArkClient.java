package xyz.juandiii.ark.mutiny;

import xyz.juandiii.ark.AbstractArkClient;
import xyz.juandiii.ark.AbstractArkBuilder;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.mutiny.http.DefaultMutinyClientRequest;
import xyz.juandiii.ark.mutiny.http.MutinyHttpTransport;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

import java.util.List;
import java.util.Objects;

public class MutinyArkClient extends AbstractArkClient<DefaultMutinyClientRequest> implements MutinyArk {

    private final MutinyHttpTransport transport;

    private MutinyArkClient(MutinyHttpTransport transport, JsonSerializer serializer,
                            String userAgent, String baseUrl,
                            List<RequestInterceptor> requestInterceptors,
                            List<ResponseInterceptor> responseInterceptors) {
        super(serializer, userAgent, baseUrl, requestInterceptors, responseInterceptors);
        this.transport = transport;
    }

    @Override
    protected DefaultMutinyClientRequest createRequest(String method, String path) {
        return new DefaultMutinyClientRequest(method, baseUrl, path, transport, serializer,
                requestInterceptors, responseInterceptors)
                .header("User-Agent", userAgent);
    }

    // ---- Builder ----

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractArkBuilder<Builder> {

        private MutinyHttpTransport transport;

        private Builder() {}

        public Builder transport(MutinyHttpTransport transport) {
            this.transport = transport;
            return this;
        }

        public MutinyArk build() {
            Objects.requireNonNull(serializer, "serializer must not be null");
            Objects.requireNonNull(transport, "transport must not be null");
            return new MutinyArkClient(transport, serializer, buildUserAgent(),
                    baseUrl, requestInterceptors, responseInterceptors);
        }
    }
}
