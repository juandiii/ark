package xyz.juandiii.ark;

import xyz.juandiii.ark.http.HttpTransport;
import xyz.juandiii.ark.http.ClientRequest;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ArkClient extends AbstractArkClient<ClientRequest> implements Ark {

    private final HttpTransport transport;

    private ArkClient(HttpTransport transport, JsonSerializer serializer, String userAgent,
                      String baseUrl,
                      List<RequestInterceptor> requestInterceptors,
                      List<ResponseInterceptor> responseInterceptors) {
        super(serializer, userAgent, baseUrl, requestInterceptors, responseInterceptors);
        this.transport = transport;
    }

    @Override
    protected ClientRequest createRequest(String method, String path) {
        return new ClientRequest(method, baseUrl, path, transport, serializer,
                requestInterceptors, responseInterceptors)
                .header("User-Agent", userAgent);
    }

    // ---- Factory method ----

    public static Builder builder() {
        return new Builder();
    }

    // ---- Abstract Builder ----

    @SuppressWarnings("unchecked")
    public abstract static class AbstractBuilder<B extends AbstractBuilder<B>> {

        protected JsonSerializer serializer;
        protected String baseUrl = "";
        protected String name = "ArkClient";
        protected String version = "1.0.0";
        protected final List<RequestInterceptor> requestInterceptors = new ArrayList<>();
        protected final List<ResponseInterceptor> responseInterceptors = new ArrayList<>();

        protected AbstractBuilder() {}

        public B serializer(JsonSerializer serializer) {
            this.serializer = serializer;
            return self();
        }

        public B baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return self();
        }

        public B userAgent(String name, String version) {
            this.name = name;
            this.version = version;
            return self();
        }

        public B requestInterceptor(RequestInterceptor interceptor) {
            this.requestInterceptors.add(interceptor);
            return self();
        }

        public B responseInterceptor(ResponseInterceptor interceptor) {
            this.responseInterceptors.add(interceptor);
            return self();
        }

        protected B self() {
            return (B) this;
        }

        protected String buildUserAgent() {
            return name + "/" + version;
        }
    }

    // ---- Builder ----

    public static final class Builder extends AbstractBuilder<Builder> {

        private HttpTransport transport;

        private Builder() {}

        public Builder transport(HttpTransport transport) {
            this.transport = transport;
            return this;
        }

        public Ark build() {
            Objects.requireNonNull(serializer, "serializer must not be null");
            Objects.requireNonNull(transport, "transport must not be null");
            return new ArkClient(transport, serializer, buildUserAgent(),
                    baseUrl, requestInterceptors, responseInterceptors);
        }
    }
}
