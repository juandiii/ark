package xyz.juandiii.spring;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import xyz.juandiii.ark.core.http.RetryPolicy;
import xyz.juandiii.ark.core.interceptor.LoggingInterceptor;
import xyz.juandiii.ark.core.proxy.HttpVersion;
import xyz.juandiii.ark.core.proxy.RegisterArkClient;

/**
 * Type-safe configuration properties for Ark HTTP clients.
 * <p>
 * Uses constructor binding (records) for native image compatibility.
 * <p>
 * Example:
 *
 * <pre>
 * ark.logging.level=BODY
 * ark.client.auth-api.base-url=https://auth.example.com
 * ark.client.auth-api.connect-timeout=5
 * ark.client.auth-api.http-version=HTTP_2
 * ark.client.auth-api.tls-configuration-name=auth-cert
 * </pre>
 *
 * @author Juan Diego Lopez V.
 */
@ConfigurationProperties(prefix = "ark")
public record ArkProperties(
        @DefaultValue Logging logging,
        @DefaultValue Map<String, ClientProperties> client
) {

    public ArkProperties {
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
            @DefaultValue Map<String, String> headers,
            RetryProperties retry
    ) {
        public ClientProperties {
            if (httpVersion == null) httpVersion = RegisterArkClient.DEFAULT_HTTP_VERSION;
            if (headers == null) headers = new LinkedHashMap<>();
        }
    }

    public record RetryProperties(
            @DefaultValue("3") int maxAttempts,
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