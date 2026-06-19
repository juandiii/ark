package xyz.juandiii.ark.core;

import xyz.juandiii.ark.core.http.ClientRequest;

/**
 * Synchronous HTTP client entry point. Provides fluent factory methods for each
 * HTTP verb that return a {@link ClientRequest} to configure headers, body, and
 * query parameters before calling {@link ClientRequest#retrieve()}.
 *
 * <p>Obtain an instance via {@link ArkClient#builder()}. Instances are
 * thread-safe; the per-request state lives on the {@link ClientRequest}.
 *
 * @author Juan Diego Lopez V.
 */
public interface Ark {

    /**
     * Start a GET request.
     *
     * @param path request path, joined with the client's base URL; may be absolute (used as-is) or relative
     * @return fluent request to configure and execute
     */
    ClientRequest get(String path);

    /** Start a GET request against the client's base URL. */
    default ClientRequest get() { return get("/"); }

    /**
     * Start a POST request.
     *
     * @param path request path, joined with the client's base URL
     * @return fluent request to configure and execute
     */
    ClientRequest post(String path);

    /** Start a POST request against the client's base URL. */
    default ClientRequest post() { return post("/"); }

    /**
     * Start a PUT request.
     *
     * @param path request path, joined with the client's base URL
     * @return fluent request to configure and execute
     */
    ClientRequest put(String path);

    /** Start a PUT request against the client's base URL. */
    default ClientRequest put() { return put("/"); }

    /**
     * Start a PATCH request.
     *
     * @param path request path, joined with the client's base URL
     * @return fluent request to configure and execute
     */
    ClientRequest patch(String path);

    /** Start a PATCH request against the client's base URL. */
    default ClientRequest patch() { return patch("/"); }

    /**
     * Start a DELETE request.
     *
     * @param path request path, joined with the client's base URL
     * @return fluent request to configure and execute
     */
    ClientRequest delete(String path);

    /** Start a DELETE request against the client's base URL. */
    default ClientRequest delete() { return delete("/"); }
}
