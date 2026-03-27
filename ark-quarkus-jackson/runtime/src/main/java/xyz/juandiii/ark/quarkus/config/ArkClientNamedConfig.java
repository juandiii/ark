package xyz.juandiii.ark.quarkus.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import xyz.juandiii.ark.proxy.HttpVersion;

import java.util.Optional;

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
    @WithDefault("HTTP_1_1")
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
}
