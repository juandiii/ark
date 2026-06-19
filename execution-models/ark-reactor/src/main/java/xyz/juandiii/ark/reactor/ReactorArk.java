package xyz.juandiii.ark.reactor;

import xyz.juandiii.ark.reactor.http.ReactorClientRequest;

/**
 * Reactive HTTP client interface returning Mono.
 *
 * @author Juan Diego Lopez V.
 */
public interface ReactorArk {

    ReactorClientRequest get(String path);

    default ReactorClientRequest get() { return get("/"); }

    ReactorClientRequest post(String path);

    default ReactorClientRequest post() { return post("/"); }

    ReactorClientRequest put(String path);

    default ReactorClientRequest put() { return put("/"); }

    ReactorClientRequest patch(String path);

    default ReactorClientRequest patch() { return patch("/"); }

    ReactorClientRequest delete(String path);

    default ReactorClientRequest delete() { return delete("/"); }
}
