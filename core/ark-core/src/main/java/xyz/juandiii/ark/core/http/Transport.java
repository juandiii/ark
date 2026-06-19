package xyz.juandiii.ark.core.http;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

/**
 * Unified transport contract parameterized on return type R. R is concrete per
 * execution model: {@link RawResponse} (sync), {@code Mono<RawResponse>} (Reactor),
 * {@code Uni<RawResponse>} (Mutiny), {@code Future<RawResponse>} (Vert.x).
 *
 * <p>ark-core declares only the contract. The reactive type wrappers live in
 * each execution-model module and never leak into ark-core's compile classpath.
 *
 * <p><b>Note:</b> {@code AsyncHttpTransport} (CompletableFuture) intentionally
 * does NOT extend {@code Transport} — see plan 017 and spike 016. The dual-mode
 * {@code ArkJdkHttpTransport} implements both {@code HttpTransport} and
 * {@code AsyncHttpTransport}; Java's nominal type system forbids one class
 * binding the same generic interface twice with different {@code R}. Async users
 * who want decorator-chain ergonomics use {@code Adapters.fromAsync(...)} from
 * the ark-async module.
 *
 * @param <R> return type wrapper (sync value, Mono, Uni, Future)
 *
 * @author Juan Diego Lopez V.
 */
public interface Transport<R> {

    /**
     * Send a text/JSON request and return the response wrapped in R.
     *
     * @param method   HTTP method (e.g. {@code "GET"}, {@code "POST"}); never {@code null}
     * @param uri      fully-qualified target URI
     * @param headers  request headers (caller-owned; transport MUST NOT mutate)
     * @param body     request body as text; {@code null} for bodiless methods
     * @param timeout  per-request timeout; {@code null} uses the underlying client's default
     * @return R wrapping the raw response (or its eventual delivery)
     */
    R send(String method, URI uri, Map<String, String> headers, String body, Duration timeout);

    /**
     * Send a binary request and return the response wrapped in R.
     * Transport implementations MUST override this when supporting binary
     * bodies; the default throws to prevent silent corruption.
     *
     * @throws UnsupportedOperationException if the transport does not implement binary upload
     */
    default R sendBinary(String method, URI uri, Map<String, String> headers,
                          byte[] body, Duration timeout) {
        throw new UnsupportedOperationException(
                "Transport must override sendBinary to preserve binary fidelity. " +
                "The default lossy implementation has been removed to prevent silent corruption.");
    }

    /**
     * Compose this transport with a decorator. Reads as
     * {@code transport.with(Retry.of(policy, ops))}.
     *
     * @param decorator function mapping this transport to a decorated one
     * @return the decorated transport
     */
    default Transport<R> with(Function<Transport<R>, Transport<R>> decorator) {
        return decorator.apply(this);
    }
}
