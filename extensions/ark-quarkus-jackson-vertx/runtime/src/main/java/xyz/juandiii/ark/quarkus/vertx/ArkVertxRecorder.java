package xyz.juandiii.ark.quarkus.vertx;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import xyz.juandiii.ark.core.AbstractArkBuilder;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.http.RetryPolicy;
import xyz.juandiii.ark.core.interceptor.LoggingInterceptor;
import xyz.juandiii.ark.core.proxy.ArkProxy;
import xyz.juandiii.ark.core.proxy.HttpVersion;
import xyz.juandiii.ark.core.proxy.InterceptorResolver;
import xyz.juandiii.ark.core.proxy.PropertyResolver;
import xyz.juandiii.ark.core.proxy.RegisterArkClient;
import xyz.juandiii.ark.core.ssl.InsecureSslContext;
import xyz.juandiii.ark.core.util.StringUtils;
import xyz.juandiii.ark.mutiny.MutinyArkClient;
import xyz.juandiii.ark.quarkus.config.ArkClientNamedConfig;
import xyz.juandiii.ark.quarkus.config.ArkClientsConfig;
import xyz.juandiii.ark.transport.vertx.mutiny.ArkVertxMutinyTransport;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Quarkus recorder that creates Mutiny-based Ark proxy client beans at runtime for
 * @RegisterArkClient interfaces whose methods return Uni/Multi.
 *
 * @author Juan Diego Lopez V.
 */
@Recorder
public class ArkVertxRecorder {

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

                ResolvedConfig resolved = resolveConfig(key, config, annotation, clientsConfig.loggingLevel());
                return buildProxy(iface, serializer, resolved);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to create Mutiny Ark client for " + interfaceName, e);
            }
        };
    }

    private record ResolvedConfig(String clientName, String baseUrl, HttpVersion httpVersion,
                                   int connectTimeout, int readTimeout, String tlsConfigName,
                                   boolean trustAll, boolean throwOnError,
                                   Map<String, String> headers,
                                   Class<?>[] interceptorClasses,
                                   RetryPolicy retryPolicy,
                                   LoggingInterceptor.Level loggingLevel) {}

    private static ResolvedConfig resolveConfig(String clientName, ArkClientNamedConfig config,
                                                 RegisterArkClient annotation,
                                                 LoggingInterceptor.Level loggingLevel) {
        return new ResolvedConfig(
                clientName,
                resolveBaseUrl(config, annotation),
                config != null ? config.httpVersion() : annotation.httpVersion(),
                config != null ? config.connectTimeout() : annotation.connectTimeout(),
                config != null ? config.readTimeout() : annotation.readTimeout(),
                config != null ? config.tlsConfigurationName().orElse(null) : null,
                config != null && config.trustAll(),
                config == null || config.throwOnError(),
                config != null ? config.headers() : Map.of(),
                annotation != null ? annotation.interceptors() : new Class<?>[0],
                resolveRetryPolicy(config),
                loggingLevel
        );
    }

    private static Object buildProxy(Class<?> iface, JsonSerializer serializer, ResolvedConfig rc) {
        MutinyArkClient.Builder builder = MutinyArkClient.builder()
                .serializer(serializer)
                .transport(buildMutinyTransport(rc))
                .baseUrl(rc.baseUrl())
                .httpVersion(rc.httpVersion())
                .connectTimeout(rc.connectTimeout())
                .readTimeout(rc.readTimeout());
        applyInterceptors(builder, rc);
        return ArkProxy.create(iface, builder.build());
    }

    private static <B extends AbstractArkBuilder<B>> void applyInterceptors(
            B builder, ResolvedConfig rc) {
        InterceptorResolver.applyHeaders(builder, rc.headers());
        InterceptorResolver.applyInterceptors(builder, rc.interceptorClasses(),
                clazz -> Arc.container().instance(clazz).get());
        LoggingInterceptor.apply(builder, rc.loggingLevel());
        builder.throwOnError(rc.throwOnError());
    }

    private static String resolveBaseUrl(ArkClientNamedConfig config, RegisterArkClient annotation) {
        if (config != null && config.baseUrl().isPresent()) {
            return config.baseUrl().get();
        }
        if (annotation == null) return "";
        return PropertyResolver.resolve(annotation.baseUrl(),
                key -> org.eclipse.microprofile.config.ConfigProvider.getConfig()
                        .getOptionalValue(key, String.class).orElse(null));
    }

    private static RetryPolicy resolveRetryPolicy(ArkClientNamedConfig config) {
        if (config == null || config.retry().maxAttempts() <= 1) return null;
        ArkClientNamedConfig.RetryConfig r = config.retry();
        return RetryPolicy.builder()
                .maxAttempts(r.maxAttempts())
                .delay(Duration.ofMillis(r.delay()))
                .multiplier(r.multiplier())
                .maxDelay(Duration.ofMillis(r.maxDelay()))
                .retryOn(r.retryOn())
                .retryOnException(r.retryOnException())
                .retryPost(r.retryPost())
                .build();
    }

    private static ArkVertxMutinyTransport buildMutinyTransport(ResolvedConfig rc) {
        Vertx vertx = Arc.container().instance(Vertx.class).get();
        WebClientOptions options = new WebClientOptions()
                .setProtocolVersion(rc.httpVersion() == HttpVersion.HTTP_2
                        ? io.vertx.core.http.HttpVersion.HTTP_2
                        : io.vertx.core.http.HttpVersion.HTTP_1_1)
                .setConnectTimeout(rc.connectTimeout() * 1000)
                .setIdleTimeout(rc.readTimeout());

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
