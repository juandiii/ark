package xyz.juandiii.ark.core.http;

import java.time.Duration;
import java.util.Set;

/**
 * Immutable retry policy with exponential backoff configuration.
 *
 * @author Juan Diego Lopez V.
 */
public final class RetryPolicy {

    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final Duration DEFAULT_DELAY = Duration.ofMillis(500);
    public static final double DEFAULT_MULTIPLIER = 2.0;
    public static final Duration DEFAULT_MAX_DELAY = Duration.ofSeconds(30);
    public static final Set<Integer> DEFAULT_RETRY_ON = Set.of(429, 502, 503, 504);
    public static final boolean DEFAULT_RETRY_ON_EXCEPTION = true;
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

    public static Builder builder() {
        return new Builder();
    }

    public static RetryPolicy defaults() {
        return builder().build();
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public Duration delay() {
        return delay;
    }

    public double multiplier() {
        return multiplier;
    }

    public Duration maxDelay() {
        return maxDelay;
    }

    public Set<Integer> retryOn() {
        return retryOn;
    }

    public boolean retryOnException() {
        return retryOnException;
    }

    public boolean retryPost() {
        return retryPost;
    }

    public static final class Builder {

        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
        private Duration delay = DEFAULT_DELAY;
        private double multiplier = DEFAULT_MULTIPLIER;
        private Duration maxDelay = DEFAULT_MAX_DELAY;
        private Set<Integer> retryOn = DEFAULT_RETRY_ON;
        private boolean retryOnException = DEFAULT_RETRY_ON_EXCEPTION;
        private boolean retryPost = DEFAULT_RETRY_POST;

        private Builder() {}

        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder delay(Duration delay) {
            this.delay = delay;
            return this;
        }

        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public Builder maxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
            return this;
        }

        public Builder retryOn(Set<Integer> retryOn) {
            this.retryOn = retryOn;
            return this;
        }

        public Builder retryOnException(boolean retryOnException) {
            this.retryOnException = retryOnException;
            return this;
        }

        public Builder retryPost(boolean retryPost) {
            this.retryPost = retryPost;
            return this;
        }

        public RetryPolicy build() {
            return new RetryPolicy(this);
        }
    }
}
