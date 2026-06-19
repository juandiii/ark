package xyz.juandiii.ark.async;

import xyz.juandiii.ark.async.http.AsyncClientRequest;

/**
 * Async HTTP client interface returning CompletableFuture.
 *
 * @author Juan Diego Lopez V.
 */
public interface AsyncArk {

    AsyncClientRequest get(String path);

    default AsyncClientRequest get() { return get("/"); }

    AsyncClientRequest post(String path);

    default AsyncClientRequest post() { return post("/"); }

    AsyncClientRequest put(String path);

    default AsyncClientRequest put() { return put("/"); }

    AsyncClientRequest patch(String path);

    default AsyncClientRequest patch() { return patch("/"); }

    AsyncClientRequest delete(String path);

    default AsyncClientRequest delete() { return delete("/"); }
}
