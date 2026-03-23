package xyz.juandiii.ark;

import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

import java.util.ArrayList;
import java.util.List;

/**
 * CRTP base builder shared across all execution models.
 *
 * @author Juan Diego Lopez V.
 */
@SuppressWarnings("unchecked")
public abstract class AbstractArkBuilder<B extends AbstractArkBuilder<B>> {

    protected JsonSerializer serializer;
    protected String baseUrl = "";
    protected String name = ArkVersion.NAME;
    protected String version = ArkVersion.VERSION;
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
