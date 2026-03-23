package xyz.juandiii.ark;

import xyz.juandiii.ark.http.ClientRequest;

/**
 * Sync HTTP client interface providing fluent request methods.
 *
 * @author Juan Diego Lopez V.
 */
public interface Ark {

    ClientRequest get(String path);

    default ClientRequest get() { return get("/"); }

    ClientRequest post(String path);

    default ClientRequest post() { return post("/"); }

    ClientRequest put(String path);

    default ClientRequest put() { return put("/"); }

    ClientRequest patch(String path);

    default ClientRequest patch() { return patch("/"); }

    ClientRequest delete(String path);

    default ClientRequest delete() { return delete("/"); }
}
