package xyz.juandiii.ark.quarkus;

import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.TrustOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import xyz.juandiii.ark.core.exceptions.ArkException;

import java.util.Optional;

/**
 * Resolves TLS configuration for Vert.x transports using Quarkus TLS Registry.
 * Returns native Vert.x TrustOptions/KeyCertOptions instead of SSLContext.
 *
 * @author Juan Diego Lopez V.
 */
@ApplicationScoped
public class QuarkusVertxTlsResolver implements VertxTlsResolver {

    @Inject
    TlsConfigurationRegistry tlsRegistry;

    @Override
    public Optional<TrustOptions> resolveTrustOptions(String tlsConfigurationName) {
        return Optional.ofNullable(getTlsConfig(tlsConfigurationName).getTrustStoreOptions());
    }

    @Override
    public Optional<KeyCertOptions> resolveKeyCertOptions(String tlsConfigurationName) {
        return Optional.ofNullable(getTlsConfig(tlsConfigurationName).getKeyStoreOptions());
    }

    private TlsConfiguration getTlsConfig(String tlsConfigurationName) {
        return tlsRegistry.get(tlsConfigurationName)
                .orElseThrow(() -> new ArkException(
                        "TLS configuration not found: " + tlsConfigurationName
                                + ". Define it in application.properties: quarkus.tls.\""
                                + tlsConfigurationName + "\".trust-store.pem.certs=..."));
    }
}