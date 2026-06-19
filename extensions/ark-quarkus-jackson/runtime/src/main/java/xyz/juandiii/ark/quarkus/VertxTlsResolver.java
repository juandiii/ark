package xyz.juandiii.ark.quarkus;

import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.TrustOptions;

import java.util.Optional;

/**
 * Interface for resolving TLS configuration as Vert.x native options.
 *
 * @author Juan Diego Lopez V.
 */
public interface VertxTlsResolver {

    Optional<TrustOptions> resolveTrustOptions(String tlsConfigurationName);

    Optional<KeyCertOptions> resolveKeyCertOptions(String tlsConfigurationName);
}