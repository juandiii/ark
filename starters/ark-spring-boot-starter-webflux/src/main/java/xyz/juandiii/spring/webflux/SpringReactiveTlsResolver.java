package xyz.juandiii.spring.webflux;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.core.proxy.TlsResolver;

import javax.net.ssl.SSLContext;

/**
 * Spring implementation of TlsResolver for reactive clients using Spring Boot SSLBundles.
 *
 * @author Juan Diego Lopez V.
 */
class SpringReactiveTlsResolver implements TlsResolver {

    private final SslBundles sslBundles;

    SpringReactiveTlsResolver(SslBundles sslBundles) {
        this.sslBundles = sslBundles;
    }

    @Override
    public SSLContext resolve(String tlsConfigurationName) {
        try {
            SslBundle bundle = sslBundles.getBundle(tlsConfigurationName);
            return bundle.createSslContext();
        } catch (Exception e) {
            throw new ArkException("Failed to resolve SSL bundle: " + tlsConfigurationName
                    + ". Define it in application.properties: spring.ssl.bundle.pem.\""
                    + tlsConfigurationName + "\".truststore.certificate=...", e);
        }
    }
}
