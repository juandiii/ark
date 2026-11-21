package xyz.juandiii.ark.quarkus.vertx;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.Transport;
import xyz.juandiii.ark.core.http.decorator.Retry;
import xyz.juandiii.ark.core.proxy.ArkProxy;
import xyz.juandiii.ark.core.proxy.HttpVersion;
import xyz.juandiii.ark.core.proxy.RegisterArkClient;
import xyz.juandiii.ark.core.ssl.InsecureSslContext;
import xyz.juandiii.ark.core.util.StringUtils;
import io.smallrye.mutiny.Uni;
import xyz.juandiii.ark.mutiny.MutinyArkClient;
import xyz.juandiii.ark.mutiny.http.decorator.MutinyRetryOps;
import xyz.juandiii.ark.quarkus.ArkRecorder;
import xyz.juandiii.ark.quarkus.ArkRecorder.ResolvedConfig;
import xyz.juandiii.ark.quarkus.config.ArkClientNamedConfig;
import xyz.juandiii.ark.quarkus.config.ArkClientsConfig;
import xyz.juandiii.ark.transport.vertx.mutiny.ArkVertxMutinyTransport;

import java.util.function.Supplier;

/**
 * Quarkus recorder that creates Mutiny-based Ark proxy client beans at runtime for
 * @RegisterArkClient interfaces whose methods return Uni/Multi.
 *
 * @author Juan Diego Lopez V.
 */
@Recorder
public class ArkVertxRecorder {

    private static final System.Logger LOGGER = System.getLogger(ArkVertxRecorder.class.getName());

    private final RuntimeValue<ArkClientsConfig> clientsConfigValue;

    public ArkVertxRecorder(RuntimeValue<ArkClientsConfig> clientsConfigValue) {
        this.clientsConfigValue = clientsConfigValue;
    }

    public Supplier<?> createMutinyArkClient(String interfaceName, String configKey) {
        return () -> {
            try {
                ArkClientsConfig clientsConfig = clientsConfigValue.getValue();
                Class<?> iface = Thread.currentThread().getContextClassLoader().loadClass(interfaceName);
                JsonSerializer serializer = Arc.container().instance(JsonSerializer.class).get();

                String key = StringUtils.isNotEmpty(configKey) ? configKey : interfaceName;
                ArkClientNamedConfig config = clientsConfig.client().get(key);
                RegisterArkClient annotation = iface.getAnnotation(RegisterArkClient.class);

                ResolvedConfig resolved = ArkRecorder.resolveConfig(key, config, annotation, clientsConfig.loggingLevel());
                return buildProxy(iface, serializer, resolved);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to create Mutiny Ark client for " + interfaceName, e);
            }
        };
    }

    private static Object buildProxy(Class<?> iface, JsonSerializer serializer, ResolvedConfig rc) {
        if (rc.maxResponseBytes() > 0) {
            LOGGER.log(System.Logger.Level.WARNING, "ark.client.\"{0}\".max-response-bytes is ignored for "
                    + "Uni/Multi clients: the Vert.x transport has no response size limit", rc.clientName());
        }
        ArkVertxMutinyTransport base = buildMutinyTransport(rc);
        Transport<Uni<RawResponse>> transport = rc.retryPolicy() != null
                ? base.with(Retry.of(rc.retryPolicy(), new MutinyRetryOps()))
                : base;
        return ArkProxy.create(iface,
                ArkRecorder.configure(MutinyArkClient.builder().transport(transport), serializer, rc).build());
    }

    private static ArkVertxMutinyTransport buildMutinyTransport(ResolvedConfig rc) {
        Vertx vertx = Arc.container().instance(Vertx.class).get();
        WebClientOptions options = new WebClientOptions()
                .setProtocolVersion(rc.httpVersion() == HttpVersion.HTTP_2
                        ? io.vertx.core.http.HttpVersion.HTTP_2
                        : io.vertx.core.http.HttpVersion.HTTP_1_1)
                .setConnectTimeout(rc.connectTimeout() * 1000)
                .setIdleTimeout(rc.readTimeout())
                .setFollowRedirects(rc.followRedirects());

        if (rc.trustAll()) {
            InsecureSslContext.warnTrustAll(rc.clientName());
            options.setSsl(true).setTrustAll(true).setVerifyHost(false);
        } else if (StringUtils.isNotEmpty(rc.tlsConfigName())) {
            VertxTlsResolver vertxTlsResolver =
                    Arc.container().instance(VertxTlsResolver.class).get();
            options.setSsl(true);
            vertxTlsResolver.resolveTrustOptions(rc.tlsConfigName()).ifPresent(options::setTrustOptions);
            vertxTlsResolver.resolveKeyCertOptions(rc.tlsConfigName()).ifPresent(options::setKeyCertOptions);
        }

        return new ArkVertxMutinyTransport(WebClient.create(vertx, options));
    }
}
