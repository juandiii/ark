package xyz.juandiii.ark.mutiny;

import xyz.juandiii.ark.mutiny.http.MutinyClientRequest;

public interface MutinyArk {

    MutinyClientRequest get(String path);

    MutinyClientRequest post(String path);

    MutinyClientRequest put(String path);

    MutinyClientRequest patch(String path);

    MutinyClientRequest delete(String path);
}
