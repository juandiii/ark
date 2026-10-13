package xyz.juandiii.ark.transport.jdk;

import xyz.juandiii.ark.core.exceptions.ResponseTooLargeException;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Body subscriber that collects the body as {@code byte[]} but aborts with a
 * {@link ResponseTooLargeException} once more than {@code maxBytes} are received.
 *
 * @author Juan Diego Lopez V.
 */
final class LimitedByteArraySubscriber implements HttpResponse.BodySubscriber<byte[]> {

    private final HttpResponse.BodySubscriber<byte[]> delegate = HttpResponse.BodySubscribers.ofByteArray();
    private final long maxBytes;
    private final long contentLength;
    private final AtomicBoolean exceeded;
    private Flow.Subscription subscription;
    private long total;
    private boolean done;

    LimitedByteArraySubscriber(long maxBytes, long contentLength) {
        this(maxBytes, contentLength, new AtomicBoolean());
    }

    LimitedByteArraySubscriber(long maxBytes, long contentLength, AtomicBoolean exceeded) {
        this.maxBytes = maxBytes;
        this.contentLength = contentLength;
        this.exceeded = exceeded;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        delegate.onSubscribe(subscription);
        if (contentLength > maxBytes) {
            abort();
        }
    }

    @Override
    public void onNext(List<ByteBuffer> items) {
        if (done) return;
        for (ByteBuffer item : items) {
            total += item.remaining();
        }
        if (total > maxBytes) {
            abort();
            return;
        }
        delegate.onNext(items);
    }

    @Override
    public void onError(Throwable throwable) {
        if (done) return;
        delegate.onError(throwable);
    }

    @Override
    public void onComplete() {
        if (done) return;
        delegate.onComplete();
    }

    @Override
    public CompletionStage<byte[]> getBody() {
        return delegate.getBody();
    }

    private void abort() {
        done = true;
        exceeded.set(true);
        delegate.onError(new ResponseTooLargeException(maxBytes));
        subscription.cancel();
    }
}
