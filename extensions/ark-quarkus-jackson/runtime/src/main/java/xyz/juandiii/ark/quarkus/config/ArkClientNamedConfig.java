package xyz.juandiii.ark.quarkus.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import xyz.juandiii.ark.core.proxy.HttpVersion;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Per-client runtime configuration.
 *
 * @author Juan Diego Lopez V.
 */
@ConfigGroup
public interface ArkClientNamedConfig {

    /**
     * Base URL of the remote service.
     */
    @WithName("base-url")
    Optional<String> baseUrl();

    /**
     * HTTP version: HTTP_1_1 or HTTP_2.
     */
    @WithName("http-version")
    @WithDefault("HTTP_2")
    HttpVersion httpVersion();

    /**
     * Connection timeout in seconds.
     */
    @WithName("connect-timeout")
    @WithDefault("10")
    int connectTimeout();

    /**
     * Read timeout in seconds.
     */
    @WithName("read-timeout")
    @WithDefault("30")
    int readTimeout();

    /**
     * TLS configuration name from Quarkus TLS Registry.
     */
    @WithName("tls-configuration-name")
    Optional<String> tlsConfigurationName();

    /**
     * Trust all SSL certificates. For development/testing only.
     */
    @WithName("trust-all")
    @WithDefault("false")
    boolean trustAll();

    /**
     * Default headers to add to every request.
     */
    @WithName("headers")
    Map<String, String> headers();

    /**
     * Retry configuration.
     */
    @WithName("retry")
    RetryConfig retry();

    @ConfigGroup
    interface RetryConfig {

        /**
         * Maximum number of retry attempts. 0 or 1 disables retry.
         */
        @WithName("max-attempts")
        @WithDefault("0")
        int maxAttempts();

        /**
         * Initial delay in milliseconds between retries.
         */
        @WithName("delay")
        @WithDefault("500")
        long delay();

        /**
         * Backoff multiplier applied to the delay after each attempt.
         */
        @WithName("multiplier")
        @WithDefault("2.0")
        double multiplier();

        /**
         * Maximum delay in milliseconds between retries.
         */
        @WithName("max-delay")
        @WithDefault("30000")
        long maxDelay();

        /**
         * HTTP status codes that trigger a retry.
         */
        @WithName("retry-on")
        @WithDefault("429,502,503,504")
        Set<Integer> retryOn();

        /**
         * Whether to retry on transport exceptions (timeout, connection errors).
         */
        @WithName("retry-on-exception")
        @WithDefault("true")
        boolean retryOnException();

        /**
         * Whether to retry non-idempotent methods (POST, PATCH).
         */
        @WithName("retry-post")
        @WithDefault("false")
        boolean retryPost();
    }
}
