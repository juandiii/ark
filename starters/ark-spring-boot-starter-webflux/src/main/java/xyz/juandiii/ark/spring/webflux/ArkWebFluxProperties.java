package xyz.juandiii.ark.spring.webflux;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import xyz.juandiii.ark.core.http.RetryPolicy;
import xyz.juandiii.ark.core.interceptor.LoggingInterceptor;
import xyz.juandiii.ark.core.proxy.HttpVersion;
import xyz.juandiii.ark.core.proxy.RegisterArkClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Type-safe configuration properties for reactive Ark HTTP clients.
 * <p>
 * Uses constructor binding (records) for native image compatibility.
 *
 * @author Juan Diego Lopez V.
 */
@ConfigurationProperties(prefix = "ark")
public record ArkWebFluxProperties(
        @DefaultValue Logging logging,
        @DefaultValue Map<String, ClientProperties> client
) {

    public ArkWebFluxProperties {
        if (logging == null) logging = new Logging(LoggingInterceptor.Level.NONE);
        if (client == null) client = new LinkedHashMap<>();
    }

    public record Logging(
            @DefaultValue("NONE") LoggingInterceptor.Level level
    ) {}

    public record ClientProperties(
            String baseUrl,
            @DefaultValue("HTTP_2") HttpVersion httpVersion,
            @DefaultValue("10") int connectTimeout,
            @DefaultValue("30") int readTimeout,
            String tlsConfigurationName,
            @DefaultValue("false") boolean trustAll,
            @DefaultValue("true") boolean throwOnError,
            @DefaultValue("true") boolean followRedirects,
            @DefaultValue Map<String, String> headers,
            RetryProperties retry
    ) {
        public ClientProperties {
            if (httpVersion == null) httpVersion = RegisterArkClient.DEFAULT_HTTP_VERSION;
            if (headers == null) headers = new LinkedHashMap<>();
        }
    }

    public record RetryProperties(
            @DefaultValue("0") int maxAttempts,
            @DefaultValue("500") long delay,
            @DefaultValue("2.0") double multiplier,
            @DefaultValue("30000") long maxDelay,
            @DefaultValue({"429", "502", "503", "504"}) Set<Integer> retryOn,
            @DefaultValue("true") boolean retryOnException,
            @DefaultValue("false") boolean retryPost
    ) {
        public RetryProperties {
            if (retryOn == null) retryOn = new LinkedHashSet<>(RetryPolicy.DEFAULT_RETRY_ON);
        }

        /**
         * Retry is off unless max-attempts is above 1, matching the sync starter and Quarkus.
         */
        public boolean enabled() {
            return maxAttempts > 1;
        }

        public RetryPolicy toRetryPolicy() {
            return RetryPolicy.builder()
                    .maxAttempts(maxAttempts)
                    .delay(Duration.ofMillis(delay))
                    .multiplier(multiplier)
                    .maxDelay(Duration.ofMillis(maxDelay))
                    .retryOn(retryOn)
                    .retryOnException(retryOnException)
                    .retryPost(retryPost)
                    .build();
        }
    }
}