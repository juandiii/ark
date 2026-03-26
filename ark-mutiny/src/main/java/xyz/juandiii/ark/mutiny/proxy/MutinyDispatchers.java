package xyz.juandiii.ark.mutiny.proxy;

import xyz.juandiii.ark.proxy.Dispatchers;
import xyz.juandiii.ark.proxy.RequestDispatcher;
import xyz.juandiii.ark.mutiny.MutinyArk;

/**
 * Factory for Mutiny RequestDispatcher.
 *
 * @author Juan Diego Lopez V.
 */
public final class MutinyDispatchers {

    private MutinyDispatchers() {}

    public static RequestDispatcher mutiny(MutinyArk ark) {
        return Dispatchers.of(ark::get, ark::post, ark::put, ark::patch, ark::delete);
    }
}