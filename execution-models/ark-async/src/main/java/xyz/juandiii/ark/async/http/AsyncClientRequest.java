package xyz.juandiii.ark.async.http;

import xyz.juandiii.ark.core.interceptor.RequestContext;

import java.time.Duration;

/**
 * Interface for async request configuration and execution.
 *
 * @author Juan Diego Lopez V.
 */
public interface AsyncClientRequest extends RequestContext {

    AsyncClientRequest accept(String mediaType);

    AsyncClientRequest contentType(String mediaType);

    AsyncClientRequest header(String key, String value);

    AsyncClientRequest queryParam(String key, String value);

    AsyncClientRequest body(Object body);

    AsyncClientRequest timeout(Duration timeout);

    /**
     * Opt out of throwing {@link xyz.juandiii.ark.core.exceptions.ApiException}
     * on HTTP error status codes (4xx/5xx). When called, the response is
     * returned to the caller unchanged regardless of status.
     *
     * @return this request for chaining
     */
    AsyncClientRequest noThrow();

    AsyncClientResponse retrieve();
}
