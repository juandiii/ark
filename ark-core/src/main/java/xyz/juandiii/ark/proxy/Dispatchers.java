package xyz.juandiii.ark.proxy;

import xyz.juandiii.ark.Ark;
import xyz.juandiii.ark.exceptions.ArkException;
import xyz.juandiii.ark.interceptor.RequestContext;

import java.util.function.Function;

/**
 * Factory for creating RequestDispatcher instances.
 *
 * @author Juan Diego Lopez V.
 */
public final class Dispatchers {

    private Dispatchers() {}

    public static RequestDispatcher sync(Ark ark) {
        return of(ark::get, ark::post, ark::put, ark::patch, ark::delete);
    }

    public static <R extends RequestContext> RequestDispatcher of(
            Function<String, R> get, Function<String, R> post,
            Function<String, R> put, Function<String, R> patch,
            Function<String, R> delete) {
        return (method, path) -> switch (method) {
            case "GET" -> get.apply(path);
            case "POST" -> post.apply(path);
            case "PUT" -> put.apply(path);
            case "PATCH" -> patch.apply(path);
            case "DELETE" -> delete.apply(path);
            default -> throw new ArkException("Unsupported HTTP method: " + method);
        };
    }
}