package xyz.juandiii.ark.mutiny.http.decorator;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.exceptions.BadRequestException;
import xyz.juandiii.ark.core.exceptions.ServiceUnavailableException;
import xyz.juandiii.ark.core.exceptions.TimeoutException;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.RetryPolicy;
import xyz.juandiii.ark.core.http.Transport;
import xyz.juandiii.ark.core.http.decorator.Retry;
import xyz.juandiii.ark.mutiny.http.MutinyHttpTransport;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MutinyRetryOpsTest {

    @Mock MutinyHttpTransport delegate;

    private static final URI TEST_URI = URI.create("https://api.example.com/test");
    private static final Map<String, String> HEADERS = Map.of();
    private static final RawResponse SUCCESS = new RawResponse(200, Map.of(), "ok");
    private static final RawResponse SERVER_ERROR = new RawResponse(503, Map.of(), "unavailable");
    private static final RawResponse BAD_REQUEST = new RawResponse(400, Map.of(), "bad");

    private Transport<Uni<RawResponse>> retried(RetryPolicy policy) {
        return new Retry<>(delegate, policy, new MutinyRetryOps());
    }

    @Test
    void givenServerError_thenRetriesAndSucceeds() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(SERVER_ERROR))
                .thenReturn(Uni.createFrom().item(SUCCESS));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(10)).build();
        RawResponse r = retried(policy).send("GET", TEST_URI, HEADERS, null, null)
                .await().atMost(Duration.ofSeconds(5));

        assertEquals(200, r.statusCode());
        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenMaxAttemptsExhausted_thenUniFailsWithApiException() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(SERVER_ERROR));

        var policy = RetryPolicy.builder().maxAttempts(2).delay(Duration.ofMillis(10)).build();
        UniAssertSubscriber<RawResponse> sub = retried(policy)
                .send("GET", TEST_URI, HEADERS, null, null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        sub.awaitFailure(Duration.ofSeconds(5));
        assertInstanceOf(ServiceUnavailableException.class, sub.getFailure());
        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenNonRetryableStatus_thenUniFailsImmediately() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(BAD_REQUEST));

        UniAssertSubscriber<RawResponse> sub = retried(RetryPolicy.defaults())
                .send("GET", TEST_URI, HEADERS, null, null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        sub.awaitFailure(Duration.ofSeconds(5));
        assertInstanceOf(BadRequestException.class, sub.getFailure());
        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenTransportException_thenRetries() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().failure(new TimeoutException("timeout", null)))
                .thenReturn(Uni.createFrom().item(SUCCESS));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(10)).build();
        RawResponse r = retried(policy).send("GET", TEST_URI, HEADERS, null, null)
                .await().atMost(Duration.ofSeconds(5));

        assertEquals(200, r.statusCode());
        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }
}
