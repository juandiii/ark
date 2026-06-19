package xyz.juandiii.ark.core.http;

import xyz.juandiii.ark.core.interceptor.RequestContext;

import java.time.Duration;

/**
 * Fluent configuration for a single synchronous HTTP request. Obtained from
 * {@link xyz.juandiii.ark.core.Ark} and terminated by {@link #retrieve()},
 * which performs the actual HTTP call and returns a {@link ClientResponse}
 * for extracting the body or wrapping into an {@code ArkResponse}.
 *
 * <p>Instances are NOT thread-safe; each request flow is single-threaded.
 *
 * @author Juan Diego Lopez V.
 */
public interface ClientRequest extends RequestContext {

    /**
     * Set the {@code Accept} header.
     *
     * @param mediaType IANA media type (e.g. {@code "application/json"})
     * @return this request for chaining
     */
    ClientRequest accept(String mediaType);

    /**
     * Set the {@code Content-Type} header.
     *
     * @param mediaType IANA media type
     * @return this request for chaining
     */
    ClientRequest contentType(String mediaType);

    /**
     * Add a request header. Calling twice with the same key overwrites the value.
     *
     * @param key   header name
     * @param value header value
     * @return this request for chaining
     */
    ClientRequest header(String key, String value);

    /**
     * Add a query parameter. Calling twice with the same key overwrites the value.
     *
     * @param key   parameter name
     * @param value parameter value
     * @return this request for chaining
     */
    ClientRequest queryParam(String key, String value);

    /**
     * Set the request body. Objects are serialized via the configured
     * {@link xyz.juandiii.ark.core.JsonSerializer}; {@code String} is sent as-is;
     * {@code byte[]} and {@link MultipartBody} are sent through the transport's
     * binary path.
     *
     * @param body request body (may be {@code null} for bodiless methods)
     * @return this request for chaining
     */
    ClientRequest body(Object body);

    /**
     * Set the per-request timeout, overriding any client-level default.
     *
     * @param timeout request timeout; {@code null} clears any per-request override
     * @return this request for chaining
     */
    ClientRequest timeout(Duration timeout);

    /**
     * Execute the HTTP request.
     *
     * @return a {@link ClientResponse} for body extraction
     * @throws xyz.juandiii.ark.core.exceptions.ArkException for transport/IO failures (timeout, connection refused, interrupted)
     * @throws xyz.juandiii.ark.core.exceptions.ApiException for HTTP status codes >= 400 (subtypes such as {@code BadRequestException}, {@code NotFoundException}, {@code ServiceUnavailableException}, etc.)
     */
    ClientResponse retrieve();
}
