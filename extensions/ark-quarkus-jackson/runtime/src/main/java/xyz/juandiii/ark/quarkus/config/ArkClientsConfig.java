package xyz.juandiii.ark.quarkus.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.*;
import xyz.juandiii.ark.core.interceptor.LoggingInterceptor;

import java.util.Map;

/**
 * Root configuration for Ark HTTP clients.
 * <p>
 * Example:
 * <pre>
 * ark.client."auth-api".base-url=https://auth.example.com
 * ark.client."auth-api".connect-timeout=5
 * ark.client."auth-api".tls-configuration-name=auth-cert
 * ark.logging.level=BODY
 * </pre>
 *
 * @author Juan Diego Lopez V.
 */
@ConfigMapping(prefix = "ark")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ArkClientsConfig {

    /**
     * Per-client configuration map.
     * Keys are either configKey values or fully qualified class names.
     * Mapped as: ark.client."key".base-url, ark.client."key".connect-timeout, etc.
     */
    Map<String, ArkClientNamedConfig> client();

    /**
     * Global logging level: NONE, BASIC, HEADERS, BODY.
     */
    @WithName("logging.level")
    @WithDefault("NONE")
    LoggingInterceptor.Level loggingLevel();
}
