package xyz.juandiii.ark.core.http;

import java.time.Duration;
import java.util.Set;

/**
 * Immutable retry policy with exponential backoff and jitter. Configures
 * {@code RetryTransport} (sync) and {@code RetryAsyncTransport} (async)
 * decorators. Only idempotent methods (GET, HEAD, PUT, DELETE, OPTIONS) are
 * retried unless {@link Builder#retryPost(boolean)} is enabled.
 *
 * @author Juan Diego Lopez V.
 */
public final class RetryPolicy {

    /** Default maximum number of attempts (including the initial call). */
    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    /** Default delay before the first retry. */
    public static final Duration DEFAULT_DELAY = Duration.ofMillis(500);
    /** Default exponential backoff multiplier per attempt. */
    public static final double DEFAULT_MULTIPLIER = 2.0;
    /** Default upper bound on the per-attempt delay. */
    public static final Duration DEFAULT_MAX_DELAY = Duration.ofSeconds(30);
    /** Default HTTP statuses considered retryable (429, 502, 503, 504). */
    public static final Set<Integer> DEFAULT_RETRY_ON = Set.of(429, 502, 503, 504);
    /** Whether to retry on transport exceptions (timeouts, connection errors). */
    public static final boolean DEFAULT_RETRY_ON_EXCEPTION = true;
    /** Whether to retry non-idempotent POST methods. Off by default for safety. */
    public static final boolean DEFAULT_RETRY_POST = false;

    private final int maxAttempts;
    private final Duration delay;
    private final double multiplier;
    private final Duration maxDelay;
    private final Set<Integer> retryOn;
    private final boolean retryOnException;
    private final boolean retryPost;

    private RetryPolicy(Builder builder) {
        this.maxAttempts = builder.maxAttempts;
        this.delay = builder.delay;
        this.multiplier = builder.multiplier;
        this.maxDelay = builder.maxDelay;
        this.retryOn = Set.copyOf(builder.retryOn);
        this.retryOnException = builder.retryOnException;
        this.retryPost = builder.retryPost;
    }

    /** @return a fresh policy builder seeded with defaults */
    public static Builder builder() {
        return new Builder();
    }

    /** @return a policy with every option at its default value */
    public static RetryPolicy defaults() {
        return builder().build();
    }

    /** @return maximum number of attempts, including the initial call */
    public int maxAttempts() {
        return maxAttempts;
    }

    /** @return base delay before the first retry */
    public Duration delay() {
        return delay;
    }

    /** @return exponential backoff multiplier */
    public double multiplier() {
        return multiplier;
    }

    /** @return upper bound on the per-attempt delay */
    public Duration maxDelay() {
        return maxDelay;
    }

    /** @return HTTP statuses that trigger a retry */
    public Set<Integer> retryOn() {
        return retryOn;
    }

    /** @return whether transport exceptions trigger a retry */
    public boolean retryOnException() {
        return retryOnException;
    }

    /** @return whether non-idempotent POST is retried */
    public boolean retryPost() {
        return retryPost;
    }

    /**
     * Mutable builder for {@link RetryPolicy}. Each setter overrides the matching default.
     */
    public static final class Builder {

        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
        private Duration delay = DEFAULT_DELAY;
        private double multiplier = DEFAULT_MULTIPLIER;
        private Duration maxDelay = DEFAULT_MAX_DELAY;
        private Set<Integer> retryOn = DEFAULT_RETRY_ON;
        private boolean retryOnException = DEFAULT_RETRY_ON_EXCEPTION;
        private boolean retryPost = DEFAULT_RETRY_POST;

        private Builder() {}

        /**
         * Set the total attempt budget. {@code 1} disables retry (initial call only).
         *
         * @param maxAttempts attempts including the initial call
         * @return this builder for chaining
         */
        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        /**
         * Set the base delay before the first retry. Subsequent delays grow by {@link #multiplier(double)}.
         *
         * @param delay base delay
         * @return this builder for chaining
         */
        public Builder delay(Duration delay) {
            this.delay = delay;
            return this;
        }

        /**
         * Set the exponential backoff multiplier.
         *
         * @param multiplier {@code >= 1} typically; {@code 1.0} yields a constant delay
         * @return this builder for chaining
         */
        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        /**
         * Cap the per-attempt delay so exponential growth doesn't run away.
         *
         * @param maxDelay upper bound
         * @return this builder for chaining
         */
        public Builder maxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
            return this;
        }

        /**
         * Replace the set of HTTP statuses that trigger a retry.
         *
         * @param retryOn status codes
         * @return this builder for chaining
         */
        public Builder retryOn(Set<Integer> retryOn) {
            this.retryOn = retryOn;
            return this;
        }

        /**
         * Toggle retrying on transport exceptions (timeouts, connection errors).
         *
         * @param retryOnException {@code true} to retry on transport-layer exceptions
         * @return this builder for chaining
         */
        public Builder retryOnException(boolean retryOnException) {
            this.retryOnException = retryOnException;
            return this;
        }

        /**
         * Allow retrying non-idempotent POST requests. Off by default — enabling this
         * can produce duplicate side-effects on the server. Set only when your endpoint
         * is intentionally idempotent (e.g. uses idempotency keys).
         *
         * @param retryPost {@code true} to retry POST
         * @return this builder for chaining
         */
        public Builder retryPost(boolean retryPost) {
            this.retryPost = retryPost;
            return this;
        }

        /** @return the immutable policy */
        public RetryPolicy build() {
            return new RetryPolicy(this);
        }
    }
}
