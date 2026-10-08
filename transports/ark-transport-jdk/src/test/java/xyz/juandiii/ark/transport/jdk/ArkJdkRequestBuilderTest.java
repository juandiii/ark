package xyz.juandiii.ark.transport.jdk;

import org.junit.jupiter.api.Test;
import xyz.juandiii.ark.core.exceptions.ResponseTooLargeException;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ArkJdkRequestBuilderTest {

    private static final class RecordingSubscription implements Flow.Subscription {
        final AtomicBoolean cancelled = new AtomicBoolean();

        @Override
        public void request(long n) {}

        @Override
        public void cancel() {
            cancelled.set(true);
        }
    }

    private static HttpResponse.ResponseInfo info(int status, String contentLength) {
        HttpHeaders headers = HttpHeaders.of(Map.of("Content-Length", List.of(contentLength)), (a, b) -> true);
        return new HttpResponse.ResponseInfo() {
            @Override
            public int statusCode() {
                return status;
            }

            @Override
            public HttpHeaders headers() {
                return headers;
            }

            @Override
            public HttpClient.Version version() {
                return HttpClient.Version.HTTP_1_1;
            }
        };
    }

    private static HttpResponse.BodySubscriber<byte[]> subscriber(String method, int status,
                                                                  RecordingSubscription subscription) {
        HttpResponse.BodySubscriber<byte[]> subscriber =
                ArkJdkRequestBuilder.bodyHandler(1_000, method).apply(info(status, "10000"));
        subscriber.onSubscribe(subscription);
        return subscriber;
    }

    private void assertNotRejected(String method, int status) throws Exception {
        RecordingSubscription subscription = new RecordingSubscription();
        HttpResponse.BodySubscriber<byte[]> subscriber = subscriber(method, status, subscription);

        subscriber.onComplete();

        assertEquals(0, subscriber.getBody().toCompletableFuture().get().length);
        assertFalse(subscription.cancelled.get());
    }

    @Test
    void contentLengthOver_get200_rejectedEarly() {
        RecordingSubscription subscription = new RecordingSubscription();
        HttpResponse.BodySubscriber<byte[]> subscriber = subscriber("GET", 200, subscription);

        subscriber.onComplete();

        CompletableFuture<byte[]> body = subscriber.getBody().toCompletableFuture();
        ExecutionException ex = assertThrows(ExecutionException.class, body::get);
        assertInstanceOf(ResponseTooLargeException.class, ex.getCause());
        assertTrue(subscription.cancelled.get());
    }

    @Test
    void contentLengthOver_get304_notRejected() throws Exception {
        assertNotRejected("GET", 304);
    }

    @Test
    void contentLengthOver_head200_notRejected() throws Exception {
        assertNotRejected("HEAD", 200);
    }

    @Test
    void contentLengthOver_get204_notRejected() throws Exception {
        assertNotRejected("GET", 204);
    }

    @Test
    void streamingOverLimit_head_stillRejected() {
        RecordingSubscription subscription = new RecordingSubscription();
        HttpResponse.BodySubscriber<byte[]> subscriber = subscriber("HEAD", 200, subscription);

        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[1_500])));

        CompletableFuture<byte[]> body = subscriber.getBody().toCompletableFuture();
        ExecutionException ex = assertThrows(ExecutionException.class, body::get);
        assertInstanceOf(ResponseTooLargeException.class, ex.getCause());
        assertTrue(subscription.cancelled.get());
    }
}
