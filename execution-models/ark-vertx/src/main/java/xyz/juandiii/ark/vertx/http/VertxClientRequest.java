package xyz.juandiii.ark.vertx.http;

import xyz.juandiii.ark.core.interceptor.RequestContext;

import java.time.Duration;

/**
 * Interface for Vert.x request configuration and execution.
 *
 * @author Juan Diego Lopez V.
 */
public interface VertxClientRequest extends RequestContext {

    VertxClientRequest accept(String mediaType);

    VertxClientRequest contentType(String mediaType);

    VertxClientRequest header(String key, String value);

    VertxClientRequest queryParam(String key, String value);

    VertxClientRequest body(Object body);

    VertxClientRequest timeout(Duration timeout);

    /**
     * Opt out of throwing {@link xyz.juandiii.ark.core.exceptions.ApiException}
     * on HTTP error status codes (4xx/5xx). When called, the response is
     * returned to the caller unchanged regardless of status.
     *
     * @return this request for chaining
     */
    VertxClientRequest noThrow();

    VertxClientResponse retrieve();
}
