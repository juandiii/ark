package xyz.juandiii.ark.async.http.decorator;

import xyz.juandiii.ark.async.http.AsyncHttpTransport;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.Transport;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Bridges {@link AsyncHttpTransport} (which intentionally does NOT extend
 * {@link Transport} — see plan 017 and spike 016) into a
 * {@code Transport<CompletableFuture<RawResponse>>} so async users can compose
 * decorators from {@code Retry<>} via {@code transport.with(...)}.
 *
 * <p>For the canonical async retry path, use {@link xyz.juandiii.ark.async.http.RetryAsyncTransport}
 * directly. This adapter is the opt-in escape hatch for users who want
 * decorator-chain ergonomics.
 *
 * @author Juan Diego Lopez V.
 */
public final class Adapters {

    private Adapters() {}

    /**
     * Wrap an {@link AsyncHttpTransport} so it satisfies {@link Transport}.
     *
     * <pre>{@code
     * Transport<CompletableFuture<RawResponse>> retried = Adapters.fromAsync(jdk)
     *         .with(Retry.of(policy, new AsyncRetryOps(scheduler)));
     * }</pre>
     *
     * @param async an AsyncHttpTransport implementation
     * @return a Transport that delegates send/sendBinary to sendAsync/sendBinaryAsync
     */
    public static Transport<CompletableFuture<RawResponse>> fromAsync(AsyncHttpTransport async) {
        return new Transport<>() {
            @Override
            public CompletableFuture<RawResponse> send(String method, URI uri,
                                                       Map<String, String> headers,
                                                       String body, Duration timeout) {
                return async.sendAsync(method, uri, headers, body, timeout);
            }

            @Override
            public CompletableFuture<RawResponse> sendBinary(String method, URI uri,
                                                              Map<String, String> headers,
                                                              byte[] body, Duration timeout) {
                return async.sendBinaryAsync(method, uri, headers, body, timeout);
            }
        };
    }

    /**
     * Reverse adapter: wrap a {@code Transport<CompletableFuture<RawResponse>>}
     * (typically the result of {@code Adapters.fromAsync(...).with(Retry.of(...))})
     * back into an {@link AsyncHttpTransport} for callers that require the
     * legacy interface — e.g. {@code AsyncArkClient.Builder.transport(...)} or
     * auto-config wiring that predates plan 017.
     *
     * <pre>{@code
     * AsyncHttpTransport withRetry = Adapters.toAsync(
     *         Adapters.fromAsync(jdk).with(Retry.of(policy, new AsyncRetryOps())));
     * AsyncArkClient client = AsyncArkClient.builder()
     *         .transport(withRetry)
     *         .build();
     * }</pre>
     *
     * @param transport a generic transport over CompletableFuture
     * @return an AsyncHttpTransport that delegates sendAsync/sendBinaryAsync to send/sendBinary
     */
    public static AsyncHttpTransport toAsync(Transport<CompletableFuture<RawResponse>> transport) {
        return new AsyncHttpTransport() {
            @Override
            public CompletableFuture<RawResponse> sendAsync(String method, URI uri,
                                                            Map<String, String> headers,
                                                            String body, Duration timeout) {
                return transport.send(method, uri, headers, body, timeout);
            }

            @Override
            public CompletableFuture<RawResponse> sendBinaryAsync(String method, URI uri,
                                                                  Map<String, String> headers,
                                                                  byte[] body, Duration timeout) {
                return transport.sendBinary(method, uri, headers, body, timeout);
            }
        };
    }
}
