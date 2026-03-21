package xyz.juandiii.ark;

import xyz.juandiii.ark.http.AsyncClientRequest;

public interface AsyncArk {

    AsyncClientRequest get(String path);

    AsyncClientRequest post(String path);

    AsyncClientRequest put(String path);

    AsyncClientRequest patch(String path);

    AsyncClientRequest delete(String path);
}
