package xyz.juandiii.ark.proxy;

import javax.net.ssl.SSLContext;

/**
 * Resolves SSL/TLS configuration by name.
 * Framework-specific implementations (Quarkus TLS Registry, Spring SSLBundles)
 * provide the actual resolution.
 *
 * @author Juan Diego Lopez V.
 */
public interface TlsResolver {

    /**
     * Resolves an SSLContext for the given TLS configuration name.
     *
     * @param tlsConfigurationName the name of the TLS configuration
     * @return the resolved SSLContext
     */
    SSLContext resolve(String tlsConfigurationName);
}
