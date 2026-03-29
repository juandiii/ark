package xyz.juandiii.ark.reactor.proxy;

import xyz.juandiii.ark.core.proxy.Dispatchers;
import xyz.juandiii.ark.core.proxy.RequestDispatcher;
import xyz.juandiii.ark.reactor.ReactorArk;

/**
 * Factory for Reactor RequestDispatcher.
 *
 * @author Juan Diego Lopez V.
 */
public final class ReactorDispatchers {

    private ReactorDispatchers() {}

    public static RequestDispatcher reactor(ReactorArk ark) {
        return Dispatchers.of(ark::get, ark::post, ark::put, ark::patch, ark::delete);
    }
}