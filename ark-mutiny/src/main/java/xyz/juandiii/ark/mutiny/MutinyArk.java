package xyz.juandiii.ark.mutiny;

import xyz.juandiii.ark.mutiny.http.MutinyClientRequest;

/**
 * Mutiny HTTP client interface returning Uni.
 *
 * @author Juan Diego Lopez V.
 */
public interface MutinyArk {

    MutinyClientRequest get(String path);

    default MutinyClientRequest get() { return get("/"); }

    MutinyClientRequest post(String path);

    default MutinyClientRequest post() { return post("/"); }

    MutinyClientRequest put(String path);

    default MutinyClientRequest put() { return put("/"); }

    MutinyClientRequest patch(String path);

    default MutinyClientRequest patch() { return patch("/"); }

    MutinyClientRequest delete(String path);

    default MutinyClientRequest delete() { return delete("/"); }
}
