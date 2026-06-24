package xyz.juandiii.ark.core;

import xyz.juandiii.ark.core.interceptor.RequestInterceptor;
import xyz.juandiii.ark.core.interceptor.ResponseInterceptor;
import xyz.juandiii.ark.core.proxy.HttpVersion;
import xyz.juandiii.ark.core.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * CRTP base shared by every Ark client builder (sync, async, reactor, mutiny,
 * vertx). Holds the configuration common to all execution models — serializer,
 * base URL, user-agent, HTTP version, timeouts, interceptors — so concrete
 * builders only declare the model-specific transport field.
 *
 * @param <B> concrete builder subtype (for fluent {@code return self()} chaining)
 *
 * @author Juan Diego Lopez V.
 */
@SuppressWarnings("unchecked")
public abstract class AbstractArkBuilder<B extends AbstractArkBuilder<B>> {

    private static final System.Logger LOGGER = System.getLogger("xyz.juandiii.ark.config");

    protected JsonSerializer serializer;
    protected String baseUrl = "";
    protected String name = ArkVersion.NAME;
    protected String version = ArkVersion.VERSION;
    protected HttpVersion httpVersion;
    protected int connectTimeoutSecs = -1;
    protected int readTimeoutSecs = -1;
    protected boolean throwOnErrorDefault = true;
    protected final List<RequestInterceptor> requestInterceptors = new ArrayList<>();
    protected final List<ResponseInterceptor> responseInterceptors = new ArrayList<>();

    protected AbstractArkBuilder() {}

    /**
     * Set the JSON serializer used for request bodies and response deserialization. Required.
     *
     * @param serializer non-null serializer
     * @return this builder for chaining
     */
    public B serializer(JsonSerializer serializer) {
        this.serializer = serializer;
        return self();
    }

    /**
     * Set the base URL that relative request paths are joined to.
     *
     * @param baseUrl absolute URL (e.g. {@code "https://api.example.com"}); trailing slash optional
     * @return this builder for chaining
     */
    public B baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return self();
    }

    /**
     * Customize the {@code User-Agent} sent on every request. Defaults to
     * Ark's own name + version.
     *
     * @param name    application name
     * @param version application version
     * @return this builder for chaining
     */
    public B userAgent(String name, String version) {
        this.name = name;
        this.version = version;
        return self();
    }

    /**
     * Configuration metadata for transports that report it.
     *
     * @param httpVersion HTTP version hint
     * @return this builder for chaining
     */
    public B httpVersion(HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
        return self();
    }

    /**
     * Configuration metadata for transports that report it.
     *
     * @param seconds connect timeout in seconds
     * @return this builder for chaining
     */
    public B connectTimeout(int seconds) {
        this.connectTimeoutSecs = seconds;
        return self();
    }

    /**
     * Configuration metadata for transports that report it.
     *
     * @param seconds read timeout in seconds
     * @return this builder for chaining
     */
    public B readTimeout(int seconds) {
        this.readTimeoutSecs = seconds;
        return self();
    }

    /**
     * Append a request interceptor. Multiple interceptors run in registration order.
     *
     * @param interceptor non-null interceptor
     * @return this builder for chaining
     */
    public B requestInterceptor(RequestInterceptor interceptor) {
        this.requestInterceptors.add(interceptor);
        return self();
    }

    /**
     * Append a response interceptor. Multiple interceptors run in registration order.
     *
     * @param interceptor non-null interceptor
     * @return this builder for chaining
     */
    public B responseInterceptor(ResponseInterceptor interceptor) {
        this.responseInterceptors.add(interceptor);
        return self();
    }

    /**
     * Set the client-level default for HTTP error behavior. When {@code true}
     * (the default), HTTP 4xx/5xx responses raise {@code ApiException}. When
     * {@code false}, the response is returned unchanged regardless of status.
     * Individual requests may still opt out via {@code request.noThrow()}.
     *
     * @param throwOnError {@code true} (default) to throw on HTTP error status, {@code false} to return the response
     * @return this builder for chaining
     */
    public B throwOnError(boolean throwOnError) {
        this.throwOnErrorDefault = throwOnError;
        return self();
    }

    protected B self() {
        return (B) this;
    }

    protected String buildUserAgent() {
        return name + "/" + version;
    }

    protected void logConfiguration(String clientType, String transportType) {
        LOGGER.log(System.Logger.Level.DEBUG, () -> formatConfiguration(clientType, transportType));
    }

    protected String formatConfiguration(String clientType, String transportType) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ark ").append(ArkVersion.VERSION).append(" - Client Configuration");
        sb.append("\n    Client: ").append(clientType);
        sb.append("\n    Transport: ").append(transportType);
        sb.append("\n    HTTP Version: ").append(httpVersion != null ? httpVersion : "(not set)");
        sb.append("\n    Base URL: ").append(StringUtils.isEmpty(baseUrl) ? "(not set)" : baseUrl);
        sb.append("\n    User-Agent: ").append(buildUserAgent());
        sb.append("\n    Serializer: ").append(serializer != null ? serializer.getClass().getSimpleName() : "(not set)");
        sb.append("\n    Connect Timeout: ").append(connectTimeoutSecs >= 0 ? connectTimeoutSecs + "s" : "(not set)");
        sb.append("\n    Read Timeout: ").append(readTimeoutSecs >= 0 ? readTimeoutSecs + "s" : "(not set)");
        sb.append("\n    Request Interceptors: ").append(requestInterceptors.size());
        sb.append("\n    Response Interceptors: ").append(responseInterceptors.size());
        return sb.toString();
    }

}
