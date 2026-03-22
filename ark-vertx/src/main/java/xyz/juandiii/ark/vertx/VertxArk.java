package xyz.juandiii.ark.vertx;

import xyz.juandiii.ark.vertx.http.VertxClientRequest;

public interface VertxArk {

    VertxClientRequest get(String path);

    VertxClientRequest post(String path);

    VertxClientRequest put(String path);

    VertxClientRequest patch(String path);

    VertxClientRequest delete(String path);
}
