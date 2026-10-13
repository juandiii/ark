package xyz.juandiii.ark.quarkus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import xyz.juandiii.ark.async.AsyncArkClient;
import xyz.juandiii.ark.core.ArkClient;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.quarkus.config.ArkClientNamedConfig;
import xyz.juandiii.ark.quarkus.config.ArkClientsConfig;

/**
 * Factory for fluent client builders pre-configured from
 * {@code ark.client."<configKey>".*} properties (base-url, timeouts, TLS,
 * headers, retry, max-response-bytes, ...).
 * <p>
 * Each builder creates a new underlying HTTP client, so build a client once and reuse it
 * instead of calling the builder per request.
 *
 * @author Juan Diego Lopez V.
 */
@ApplicationScoped
public class ArkClients {

    @Inject
    ArkClientsConfig clientsConfig;

    @Inject
    JsonSerializer serializer;

    /**
     * Fluent builder pre-configured from {@code ark.client."<configKey>".*}.
     * Each call returns a new builder and a new underlying HTTP client.
     *
     * @param configKey the key under {@code ark.client."<configKey>".*}
     * @return a configured sync builder
     * @throws ArkException if no {@code ark.client."<configKey>".*} configuration exists
     */
    public ArkClient.Builder builder(String configKey) {
        ArkRecorder.ResolvedConfig rc = resolve(configKey);
        return ArkRecorder.syncBuilder(serializer, rc);
    }

    /**
     * Async variant of {@link #builder(String)}.
     *
     * @param configKey the key under {@code ark.client."<configKey>".*}
     * @return a configured async builder
     * @throws ArkException if no {@code ark.client."<configKey>".*} configuration exists
     */
    public AsyncArkClient.Builder asyncBuilder(String configKey) {
        ArkRecorder.ResolvedConfig rc = resolve(configKey);
        return ArkRecorder.asyncBuilder(serializer, rc);
    }

    private ArkRecorder.ResolvedConfig resolve(String configKey) {
        ArkClientNamedConfig config = clientsConfig.client().get(configKey);
        if (config == null) {
            throw new ArkException("No Ark client configuration for key '" + configKey
                    + "' (expected ark.client.\"" + configKey + "\".* properties)");
        }
        return ArkRecorder.resolveConfig(configKey, config, null, clientsConfig.loggingLevel());
    }
}
