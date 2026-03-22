package xyz.juandiii.ark.reactor;

import xyz.juandiii.ark.reactor.http.ReactorClientRequest;

public interface ReactorArk {

    ReactorClientRequest get(String path);

    ReactorClientRequest post(String path);

    ReactorClientRequest put(String path);

    ReactorClientRequest patch(String path);

    ReactorClientRequest delete(String path);
}
