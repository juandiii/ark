package xyz.juandiii.ark.mutiny.http;

import xyz.juandiii.ark.core.interceptor.RequestContext;

import java.time.Duration;

/**
 * Interface for Mutiny request configuration and execution.
 *
 * @author Juan Diego Lopez V.
 */
public interface MutinyClientRequest extends RequestContext {

    MutinyClientRequest accept(String mediaType);

    MutinyClientRequest contentType(String mediaType);

    MutinyClientRequest header(String key, String value);

    MutinyClientRequest queryParam(String key, String value);

    MutinyClientRequest body(Object body);

    MutinyClientRequest timeout(Duration timeout);

    /**
     * Opt out of throwing {@link xyz.juandiii.ark.core.exceptions.ApiException}
     * on HTTP error status codes (4xx/5xx). When called, the response is
     * returned to the caller unchanged regardless of status.
     *
     * @return this request for chaining
     */
    MutinyClientRequest noThrow();

    MutinyClientResponse retrieve();
}
