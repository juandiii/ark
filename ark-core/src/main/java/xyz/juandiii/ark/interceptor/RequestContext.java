package xyz.juandiii.ark.interceptor;

import java.time.Duration;
import java.util.Map;

/**
 * Interface exposing request state for interceptors.
 *
 * @author Juan Diego Lopez V.
 */
public interface RequestContext {

    // ---- getters ----

    String method();

    String path();

    Map<String, String> headers();

    Map<String, String> queryParams();

    Object body();

    Duration timeout();

    // ---- setters (fluent) ----

    RequestContext accept(String mediaType);

    RequestContext contentType(String mediaType);

    RequestContext header(String key, String value);

    RequestContext queryParam(String key, String value);

    RequestContext body(Object body);

    RequestContext timeout(Duration timeout);
}
