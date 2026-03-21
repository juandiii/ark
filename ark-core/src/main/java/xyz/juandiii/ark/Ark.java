package xyz.juandiii.ark;

import xyz.juandiii.ark.http.ClientRequest;

public interface Ark {

    ClientRequest get(String path);

    ClientRequest post(String path);

    ClientRequest put(String path);

    ClientRequest patch(String path);

    ClientRequest delete(String path);
}
