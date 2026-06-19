package xyz.juandiii.ark.async.proxy;

import xyz.juandiii.ark.async.AsyncArk;
import xyz.juandiii.ark.core.proxy.Dispatchers;
import xyz.juandiii.ark.core.proxy.RequestDispatcher;

/**
 * Factory for async RequestDispatcher.
 *
 * @author Juan Diego Lopez V.
 */
public final class AsyncDispatchers {

    private AsyncDispatchers() {}

    public static RequestDispatcher async(AsyncArk ark) {
        return Dispatchers.of(ark::get, ark::post, ark::put, ark::patch, ark::delete);
    }
}