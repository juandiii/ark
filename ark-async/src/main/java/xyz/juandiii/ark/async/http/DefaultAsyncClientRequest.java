package xyz.juandiii.ark.async.http;

import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.http.AbstractClientRequest;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.Transport;
import xyz.juandiii.ark.core.interceptor.RequestInterceptor;
import xyz.juandiii.ark.core.interceptor.ResponseInterceptor;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Default implementation of {@link AsyncClientRequest}.
 *
 * @author Juan Diego Lopez V.
 */
public final class DefaultAsyncClientRequest extends AbstractClientRequest<DefaultAsyncClientRequest> implements AsyncClientRequest {

    private final Transport<CompletableFuture<RawResponse>> transport;

    public DefaultAsyncClientRequest(String method, String baseUrl, String path,
                                     Transport<CompletableFuture<RawResponse>> transport,
                                     JsonSerializer serializer,
                                     List<RequestInterceptor> requestInterceptors,
                                     List<ResponseInterceptor> responseInterceptors) {
        super(method, baseUrl, path, serializer, requestInterceptors, responseInterceptors);
        this.transport = transport;
    }

    @Override
    public AsyncClientResponse retrieve() {
        applyInterceptors();
        SerializedBody body = prepareBody();

        // Capture caller stack on the calling thread. CompletableFuture continuations
        // run on a different thread (ForkJoinPool / transport executor), so the
        // exception's native stacktrace shows only Ark / CF machinery — never the
        // user code that initiated the call. Attaching this as a suppressed
        // exception preserves the call site in the printed stacktrace.
        Throwable callSite = new Throwable("Async call originated here");

        CompletableFuture<RawResponse> future = body.isBinary()
                ? transport.sendBinary(method, buildUri(), headers, body.binary(), timeout)
                : transport.send(method, buildUri(), headers, body.text(), timeout);
        for (ResponseInterceptor interceptor : responseInterceptors) {
            future = future.thenApply(interceptor::intercept);
        }
        future = future.thenApply(raw -> { validateResponse(raw); return raw; });
        future = future.whenComplete((raw, throwable) -> {
            if (throwable != null) {
                unwrap(throwable).addSuppressed(callSite);
            }
        });
        return new DefaultAsyncClientResponse(future, serializer);
    }

    private static Throwable unwrap(Throwable t) {
        return t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
    }
}
