package xyz.juandiii.ark.http;

import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.interceptor.RequestContext;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@SuppressWarnings("unchecked")
public abstract class AbstractClientRequest<T extends AbstractClientRequest<T>>
        implements RequestContext {

    protected final String method;
    protected final String baseUrl;
    protected final String path;
    protected final Map<String, String> headers = new LinkedHashMap<>();
    protected final Map<String, String> queryParams = new LinkedHashMap<>();
    protected Object body;
    protected Duration timeout;
    protected final JsonSerializer serializer;
    protected final List<RequestInterceptor> requestInterceptors;
    protected final List<ResponseInterceptor> responseInterceptors;

    protected AbstractClientRequest(String method, String baseUrl, String path,
                                    JsonSerializer serializer,
                                    List<RequestInterceptor> requestInterceptors,
                                    List<ResponseInterceptor> responseInterceptors) {
        this.method = method;
        this.baseUrl = baseUrl;
        this.path = path;
        this.serializer = serializer;
        this.requestInterceptors = requestInterceptors;
        this.responseInterceptors = responseInterceptors;
    }

    @Override
    public T accept(String mediaType) {
        headers.put("Accept", mediaType);
        return self();
    }

    @Override
    public T contentType(String mediaType) {
        headers.put("Content-Type", mediaType);
        return self();
    }

    @Override
    public T header(String key, String value) {
        headers.put(key, value);
        return self();
    }

    @Override
    public T queryParam(String key, String value) {
        if (value != null) {
            queryParams.put(key, value);
        }
        return self();
    }

    @Override
    public T body(Object body) {
        this.body = body;
        return self();
    }

    @Override
    public T timeout(Duration timeout) {
        this.timeout = timeout;
        return self();
    }

    protected String prepareBody() {
        requestInterceptors.forEach(interceptor -> interceptor.intercept(this));
        return needsBody() ? serializer.serialize(body) : null;
    }

    protected URI buildUri() {
        String url = baseUrl + path;
        if (!queryParams.isEmpty()) {
            StringJoiner joiner = new StringJoiner("&");
            queryParams.forEach((k, v) -> joiner.add(encode(k) + "=" + encode(v)));
            url += "?" + joiner;
        }
        return URI.create(url);
    }

    protected boolean needsBody() {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private T self() {
        return (T) this;
    }
}
