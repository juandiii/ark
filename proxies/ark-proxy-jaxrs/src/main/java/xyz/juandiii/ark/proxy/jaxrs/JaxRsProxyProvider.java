package xyz.juandiii.ark.proxy.jaxrs;

import xyz.juandiii.ark.core.proxy.AnnotationResolver;
import xyz.juandiii.ark.core.proxy.ArkProxy;
import xyz.juandiii.ark.core.proxy.ParameterBinder;

/**
 * Provides JAX-RS annotation resolver and parameter binder for ArkProxy auto-detection.
 *
 * @author Juan Diego Lopez V.
 */
public final class JaxRsProxyProvider implements ArkProxy.ProxyProviderAccess {

    @Override
    public AnnotationResolver annotationResolver() {
        return new JaxRsAnnotationResolver();
    }

    @Override
    public ParameterBinder parameterBinder() {
        return new JaxRsParameterBinder();
    }
}