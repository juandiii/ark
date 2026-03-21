package xyz.juandiii.ark;

import xyz.juandiii.ark.http.AsyncHttpTransport;
import xyz.juandiii.ark.http.HttpTransport;
import xyz.juandiii.ark.http.ClientRequest;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ArkClient implements Ark {

    private final HttpTransport transport;
    private final JsonSerializer serializer;
    private final String userAgent;
    private final String baseUrl;
    private final List<RequestInterceptor> requestInterceptors;
    private final List<ResponseInterceptor> responseInterceptors;

    private ArkClient(HttpTransport transport, JsonSerializer serializer, String userAgent,
                      String baseUrl,
                      List<RequestInterceptor> requestInterceptors,
                      List<ResponseInterceptor> responseInterceptors) {
        this.transport = transport;
        this.serializer = serializer;
        this.userAgent = userAgent;
        this.baseUrl = baseUrl;
        this.requestInterceptors = List.copyOf(requestInterceptors);
        this.responseInterceptors = List.copyOf(responseInterceptors);
    }

    // ---- Ark interface ----

    @Override
    public ClientRequest get(String path) {
        return spec("GET", path);
    }

    @Override
    public ClientRequest post(String path) {
        return spec("POST", path);
    }

    @Override
    public ClientRequest put(String path) {
        return spec("PUT", path);
    }

    @Override
    public ClientRequest patch(String path) {
        return spec("PATCH", path);
    }

    @Override
    public ClientRequest delete(String path) {
        return spec("DELETE", path);
    }

    private ClientRequest spec(String method, String path) {
        return new ClientRequest(method, baseUrl, path, transport, serializer,
                requestInterceptors, responseInterceptors)
                .header("User-Agent", userAgent);
    }

    // ---- Factory methods ----

    public static SyncBuilder sync() {
        return new SyncBuilder();
    }

    public static AsyncBuilder async() {
        return new AsyncBuilder();
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

    // ---- Sync Builder ----

    public static final class SyncBuilder extends AbstractBuilder<SyncBuilder> {

        private HttpTransport transport;

        private SyncBuilder() {}

        public SyncBuilder transport(HttpTransport transport) {
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

    // ---- Async Builder ----

    public static final class AsyncBuilder extends AbstractBuilder<AsyncBuilder> {

        private AsyncHttpTransport transport;

        private AsyncBuilder() {}

        public AsyncBuilder transport(AsyncHttpTransport transport) {
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