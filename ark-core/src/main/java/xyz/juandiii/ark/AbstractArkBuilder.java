package xyz.juandiii.ark;

import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;
import xyz.juandiii.ark.proxy.HttpVersion;
import xyz.juandiii.ark.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * CRTP base builder shared across all execution models.
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
    protected final List<RequestInterceptor> requestInterceptors = new ArrayList<>();
    protected final List<ResponseInterceptor> responseInterceptors = new ArrayList<>();

    protected AbstractArkBuilder() {}

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

    public B httpVersion(HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
        return self();
    }

    public B connectTimeout(int seconds) {
        this.connectTimeoutSecs = seconds;
        return self();
    }

    public B readTimeout(int seconds) {
        this.readTimeoutSecs = seconds;
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

    protected void logConfiguration(String clientType, String transportType) {
        LOGGER.log(System.Logger.Level.DEBUG, () -> formatConfiguration(clientType, transportType));
    }

    protected String formatConfiguration(String clientType, String transportType) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ark ").append(ArkVersion.VERSION).append(" — Client Configuration");
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
