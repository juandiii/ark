package xyz.juandiii.ark.spring.webflux;

import xyz.juandiii.ark.spring.AbstractArkRuntimeHints;

/**
 * Registers Ark classes for reflection in GraalVM native image; see {@link AbstractArkRuntimeHints}.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkWebFluxClientRuntimeHints extends AbstractArkRuntimeHints {

    public ArkWebFluxClientRuntimeHints() {
        super(ArkWebFluxProperties.class,
                "xyz.juandiii.ark.reactor.proxy.ReactorExecutionModelProvider",
                "xyz.juandiii.ark.reactor.proxy.ReactorReturnTypeHandler");
    }
}
