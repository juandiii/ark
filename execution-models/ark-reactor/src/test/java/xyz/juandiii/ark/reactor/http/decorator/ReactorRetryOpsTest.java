package xyz.juandiii.ark.reactor.http.decorator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.juandiii.ark.core.exceptions.BadRequestException;
import xyz.juandiii.ark.core.exceptions.ServiceUnavailableException;
import xyz.juandiii.ark.core.exceptions.TimeoutException;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.RetryPolicy;
import xyz.juandiii.ark.core.http.Transport;
import xyz.juandiii.ark.core.http.decorator.Retry;
import xyz.juandiii.ark.reactor.http.ReactorHttpTransport;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * H3 reactor gate (plan 017): verifies Retry&lt;Mono&lt;RawResponse&gt;&gt; +
 * ReactorRetryOps composes correctly with Reactor's flatMap / onErrorResume /
 * delaySubscription primitives.
 */
@ExtendWith(MockitoExtension.class)
class ReactorRetryOpsTest {

    @Mock ReactorHttpTransport delegate;

    private static final URI TEST_URI = URI.create("https://api.example.com/test");
    private static final Map<String, String> HEADERS = Map.of();
    private static final RawResponse SUCCESS = new RawResponse(200, Map.of(), "ok");
    private static final RawResponse SERVER_ERROR = new RawResponse(503, Map.of(), "unavailable");
    private static final RawResponse BAD_REQUEST = new RawResponse(400, Map.of(), "bad");

    private Transport<Mono<RawResponse>> retried(RetryPolicy policy) {
        return new Retry<>(delegate, policy, new ReactorRetryOps());
    }

    @Test
    void givenServerError_thenRetriesAndSucceeds() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(Mono.just(SERVER_ERROR))
                .thenReturn(Mono.just(SUCCESS));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(10)).build();

        StepVerifier.create(retried(policy).send("GET", TEST_URI, HEADERS, null, null))
                .assertNext(r -> {
                    org.junit.jupiter.api.Assertions.assertEquals(200, r.statusCode());
                })
                .verifyComplete();

        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenMaxAttemptsExhausted_thenEmitsApiException() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(Mono.just(SERVER_ERROR));

        var policy = RetryPolicy.builder().maxAttempts(2).delay(Duration.ofMillis(10)).build();

        StepVerifier.create(retried(policy).send("GET", TEST_URI, HEADERS, null, null))
                .expectError(ServiceUnavailableException.class)
                .verify();

        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenNonRetryableStatus_thenEmitsImmediately() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(Mono.just(BAD_REQUEST));

        StepVerifier.create(retried(RetryPolicy.defaults()).send("GET", TEST_URI, HEADERS, null, null))
                .expectError(BadRequestException.class)
                .verify();

        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenTransportException_thenRetries() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(Mono.error(new TimeoutException("timeout", null)))
                .thenReturn(Mono.just(SUCCESS));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(10)).build();

        StepVerifier.create(retried(policy).send("GET", TEST_URI, HEADERS, null, null))
                .assertNext(r -> org.junit.jupiter.api.Assertions.assertEquals(200, r.statusCode()))
                .verifyComplete();

        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenPostMethod_thenNoRetryByDefault() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(Mono.just(SERVER_ERROR));

        StepVerifier.create(retried(RetryPolicy.defaults()).send("POST", TEST_URI, HEADERS, "body", null))
                .expectError(ServiceUnavailableException.class)
                .verify();

        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
    }
}
