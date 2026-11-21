package xyz.juandiii.ark.spring;

/**
 * Registers Ark classes for reflection in GraalVM native image; see {@link AbstractArkRuntimeHints}.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkClientRuntimeHints extends AbstractArkRuntimeHints {

    public ArkClientRuntimeHints() {
        super(ArkProperties.class, "xyz.juandiii.ark.core.proxy.SyncReturnTypeHandler");
    }
}
