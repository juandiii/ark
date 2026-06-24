package xyz.juandiii.ark.spring.webflux;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import xyz.juandiii.ark.core.interceptor.LoggingInterceptor;
import xyz.juandiii.ark.core.proxy.HttpVersion;
import xyz.juandiii.ark.core.proxy.RegisterArkClient;

import java.util.LinkedHashMap;
import java.util.Map;

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
            @DefaultValue Map<String, String> headers
    ) {
        public ClientProperties {
            if (httpVersion == null) httpVersion = RegisterArkClient.DEFAULT_HTTP_VERSION;
            if (headers == null) headers = new LinkedHashMap<>();
        }
    }
}