package xyz.juandiii.spring;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import xyz.juandiii.ark.core.http.RetryPolicy;
import xyz.juandiii.ark.core.interceptor.LoggingInterceptor;
import xyz.juandiii.ark.core.proxy.HttpVersion;
import xyz.juandiii.ark.core.proxy.RegisterArkClient;

/**
 * Type-safe configuration properties for Ark HTTP clients.
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
public class ArkProperties {

    private final Logging logging = new Logging();
    private Map<String, ClientProperties> client = new LinkedHashMap<>();

    public Logging getLogging() {
        return logging;
    }

    public Map<String, ClientProperties> getClient() {
        return client;
    }

    public void setClient(Map<String, ClientProperties> client) {
        this.client = client;
    }

    public static class Logging {

        private LoggingInterceptor.Level level = LoggingInterceptor.Level.NONE;

        public LoggingInterceptor.Level getLevel() {
            return level;
        }

        public void setLevel(LoggingInterceptor.Level level) {
            this.level = level;
        }
    }

    public static class ClientProperties {

        private String baseUrl;
        private HttpVersion httpVersion = RegisterArkClient.DEFAULT_HTTP_VERSION;
        private int connectTimeout = RegisterArkClient.DEFAULT_CONNECT_TIMEOUT;
        private int readTimeout = RegisterArkClient.DEFAULT_READ_TIMEOUT;
        private String tlsConfigurationName;
        private boolean trustAll;
        private Map<String, String> headers = new LinkedHashMap<>();
        private RetryProperties retry;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public HttpVersion getHttpVersion() {
            return httpVersion;
        }

        public void setHttpVersion(HttpVersion httpVersion) {
            this.httpVersion = httpVersion;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }

        public String getTlsConfigurationName() {
            return tlsConfigurationName;
        }

        public void setTlsConfigurationName(String tlsConfigurationName) {
            this.tlsConfigurationName = tlsConfigurationName;
        }

        public boolean isTrustAll() {
            return trustAll;
        }

        public void setTrustAll(boolean trustAll) {
            this.trustAll = trustAll;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        public RetryProperties getRetry() {
            return retry;
        }

        public void setRetry(RetryProperties retry) {
            this.retry = retry;
        }
    }

    public static class RetryProperties {

        private int maxAttempts = RetryPolicy.DEFAULT_MAX_ATTEMPTS;
        private long delay = RetryPolicy.DEFAULT_DELAY.toMillis();
        private double multiplier = RetryPolicy.DEFAULT_MULTIPLIER;
        private long maxDelay = RetryPolicy.DEFAULT_MAX_DELAY.toMillis();
        private Set<Integer> retryOn = new LinkedHashSet<>(RetryPolicy.DEFAULT_RETRY_ON);
        private boolean retryOnException = RetryPolicy.DEFAULT_RETRY_ON_EXCEPTION;
        private boolean retryPost = RetryPolicy.DEFAULT_RETRY_POST;

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

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public long getDelay() { return delay; }
        public void setDelay(long delay) { this.delay = delay; }
        public double getMultiplier() { return multiplier; }
        public void setMultiplier(double multiplier) { this.multiplier = multiplier; }
        public long getMaxDelay() { return maxDelay; }
        public void setMaxDelay(long maxDelay) { this.maxDelay = maxDelay; }
        public Set<Integer> getRetryOn() { return retryOn; }
        public void setRetryOn(Set<Integer> retryOn) { this.retryOn = retryOn; }
        public boolean isRetryOnException() { return retryOnException; }
        public void setRetryOnException(boolean retryOnException) { this.retryOnException = retryOnException; }
        public boolean isRetryPost() { return retryPost; }
        public void setRetryPost(boolean retryPost) { this.retryPost = retryPost; }
    }
}