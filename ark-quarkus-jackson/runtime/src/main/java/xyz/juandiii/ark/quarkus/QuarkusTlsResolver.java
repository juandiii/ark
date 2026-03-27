package xyz.juandiii.ark.quarkus;

import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import xyz.juandiii.ark.exceptions.ArkException;
import xyz.juandiii.ark.proxy.TlsResolver;

import javax.net.ssl.SSLContext;
import java.util.Optional;

/**
 * Quarkus implementation of TlsResolver using Quarkus TLS Registry.
 * Resolves named TLS configurations defined in application.properties:
 * quarkus.tls."name".trust-store.pem.certs=./certs/ca.pem
 *
 * @author Juan Diego Lopez V.
 */
@ApplicationScoped
public class QuarkusTlsResolver implements TlsResolver {

    @Inject
    TlsConfigurationRegistry tlsRegistry;

    @Override
    public SSLContext resolve(String tlsConfigurationName) {
        Optional<TlsConfiguration> config = tlsRegistry.get(tlsConfigurationName);
        if (config.isEmpty()) {
            throw new ArkException("TLS configuration not found: " + tlsConfigurationName
                    + ". Define it in application.properties: quarkus.tls.\""
                    + tlsConfigurationName + "\".trust-store.pem.certs=...");
        }
        try {
            return config.get().createSSLContext();
        } catch (Exception e) {
            throw new ArkException("Failed to create SSLContext for TLS configuration: "
                    + tlsConfigurationName, e);
        }
    }
}
