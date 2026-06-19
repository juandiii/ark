package xyz.juandiii.ark.vertx;

import xyz.juandiii.ark.vertx.http.VertxClientRequest;

/**
 * Vert.x HTTP client interface returning Future.
 *
 * @author Juan Diego Lopez V.
 */
public interface VertxArk {

    VertxClientRequest get(String path);

    default VertxClientRequest get() { return get("/"); }

    VertxClientRequest post(String path);

    default VertxClientRequest post() { return post("/"); }

    VertxClientRequest put(String path);

    default VertxClientRequest put() { return put("/"); }

    VertxClientRequest patch(String path);

    default VertxClientRequest patch() { return patch("/"); }

    VertxClientRequest delete(String path);

    default VertxClientRequest delete() { return delete("/"); }
}
