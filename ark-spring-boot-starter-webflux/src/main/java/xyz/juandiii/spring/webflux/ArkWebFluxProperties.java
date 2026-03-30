package xyz.juandiii.spring.webflux;

import org.springframework.boot.context.properties.ConfigurationProperties;
import xyz.juandiii.ark.core.interceptor.LoggingInterceptor;
import xyz.juandiii.ark.core.proxy.HttpVersion;
import xyz.juandiii.ark.core.proxy.RegisterArkClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Type-safe configuration properties for reactive Ark HTTP clients.
 *
 * @author Juan Diego Lopez V.
 */
@ConfigurationProperties(prefix = "ark")
public class ArkWebFluxProperties {

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

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public HttpVersion getHttpVersion() { return httpVersion; }
        public void setHttpVersion(HttpVersion httpVersion) { this.httpVersion = httpVersion; }
        public int getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; }
        public int getReadTimeout() { return readTimeout; }
        public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
        public String getTlsConfigurationName() { return tlsConfigurationName; }
        public void setTlsConfigurationName(String tlsConfigurationName) { this.tlsConfigurationName = tlsConfigurationName; }
        public boolean isTrustAll() { return trustAll; }
        public void setTrustAll(boolean trustAll) { this.trustAll = trustAll; }
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    }
}
