package xyz.juandiii.ark.interceptor;

import java.time.Duration;

public interface RequestContext {

    RequestContext accept(String mediaType);

    RequestContext contentType(String mediaType);

    RequestContext header(String key, String value);

    RequestContext queryParam(String key, String value);

    RequestContext body(Object body);

    RequestContext timeout(Duration timeout);
}
