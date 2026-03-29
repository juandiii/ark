package xyz.juandiii.ark.core.http;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RetryPolicyTest {

    @Test
    void givenDefaults_thenAllDefaultValuesAreSet() {
        RetryPolicy policy = RetryPolicy.defaults();

        assertEquals(3, policy.maxAttempts());
        assertEquals(Duration.ofMillis(500), policy.delay());
        assertEquals(2.0, policy.multiplier());
        assertEquals(Duration.ofSeconds(30), policy.maxDelay());
        assertEquals(Set.of(429, 502, 503, 504), policy.retryOn());
        assertTrue(policy.retryOnException());
        assertFalse(policy.retryPost());
    }

    @Test
    void givenCustomValues_thenCustomValuesAreSet() {
        RetryPolicy policy = RetryPolicy.builder()
                .maxAttempts(5)
                .delay(Duration.ofMillis(1000))
                .multiplier(3.0)
                .maxDelay(Duration.ofMinutes(1))
                .retryOn(Set.of(500, 503))
                .retryOnException(false)
                .retryPost(true)
                .build();

        assertEquals(5, policy.maxAttempts());
        assertEquals(Duration.ofMillis(1000), policy.delay());
        assertEquals(3.0, policy.multiplier());
        assertEquals(Duration.ofMinutes(1), policy.maxDelay());
        assertEquals(Set.of(500, 503), policy.retryOn());
        assertFalse(policy.retryOnException());
        assertTrue(policy.retryPost());
    }

    @Test
    void givenRetryOnSet_thenDefensivelyCopied() {
        var mutableSet = new java.util.HashSet<>(Set.of(429, 503));
        RetryPolicy policy = RetryPolicy.builder().retryOn(mutableSet).build();
        mutableSet.add(999);

        assertFalse(policy.retryOn().contains(999));
    }

    @Test
    void givenRetryOnSet_thenImmutable() {
        RetryPolicy policy = RetryPolicy.defaults();

        assertThrows(UnsupportedOperationException.class, () ->
                policy.retryOn().add(999));
    }
}
