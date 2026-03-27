package xyz.juandiii.ark.quarkus;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import xyz.juandiii.ark.ArkClient;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.async.AsyncArkClient;
import xyz.juandiii.ark.interceptor.LoggingInterceptor;
import xyz.juandiii.ark.mutiny.MutinyArkClient;
import xyz.juandiii.ark.proxy.RegisterArkClient;
import xyz.juandiii.ark.proxy.TlsResolver;
import xyz.juandiii.ark.proxy.jaxrs.ArkJaxRsProxy;
import xyz.juandiii.ark.quarkus.config.ArkClientNamedConfig;
import xyz.juandiii.ark.quarkus.config.ArkClientsConfig;
import xyz.juandiii.ark.transport.jdk.ArkJdkHttpTransport;
import xyz.juandiii.ark.transport.vertx.mutiny.ArkVertxMutinyTransport;
import xyz.juandiii.ark.util.StringUtils;

import javax.net.ssl.SSLContext;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Quarkus recorder that creates Ark proxy client beans at runtime.
 * Uses typed @ConfigMapping configuration instead of manual property lookup.
 *
 * @author Juan Diego Lopez V.
 */
@Recorder
public class ArkRecorder {

    private final RuntimeValue<ArkClientsConfig> clientsConfigValue;

    public ArkRecorder(RuntimeValue<ArkClientsConfig> clientsConfigValue) {
        this.clientsConfigValue = clientsConfigValue;
    }

    public Supplier<?> createArkClient(String interfaceName, String configKey) {
        return () -> {
            try {
                ArkClientsConfig clientsConfig = clientsConfigValue.getValue();
                Class<?> iface = Thread.currentThread().getContextClassLoader().loadClass(interfaceName);
                JsonSerializer serializer = Arc.container().instance(JsonSerializer.class).get();

                // Resolve config: configKey → FQCN → annotation defaults
                String key = StringUtils.isNotEmpty(configKey) ? configKey : interfaceName;
                ArkClientNamedConfig config = clientsConfig.client().get(key);
                RegisterArkClient annotation = iface.getAnnotation(RegisterArkClient.class);

                String baseUrl = resolveBaseUrl(config, annotation);
                String httpVersion = config != null ? config.httpVersion() : annotation.httpVersion().name();
                int connectTimeout = config != null ? config.connectTimeout() : annotation.connectTimeout();
                int readTimeout = config != null ? config.readTimeout() : annotation.readTimeout();
                String tlsConfigName = config != null ? config.tlsConfigurationName().orElse(null) : null;

                LoggingInterceptor.Level loggingLevel = LoggingInterceptor.parseLevel(clientsConfig.loggingLevel());

                if (usesReactiveReturnTypes(iface)) {
                    MutinyArkClient.Builder builder = MutinyArkClient.builder()
                            .serializer(serializer)
                            .transport(buildMutinyTransport(httpVersion, connectTimeout, readTimeout, tlsConfigName))
                            .baseUrl(baseUrl)
                            .connectTimeout(connectTimeout)
                            .readTimeout(readTimeout);
                    LoggingInterceptor.apply(builder, loggingLevel);
                    return ArkJaxRsProxy.create(iface, builder.build());
                } else if (usesAsyncReturnTypes(iface)) {
                    SSLContext sslContext = resolveTls(tlsConfigName);
                    AsyncArkClient.Builder builder = AsyncArkClient.builder()
                            .serializer(serializer)
                            .transport(buildJdkTransport(httpVersion, connectTimeout, sslContext))
                            .baseUrl(baseUrl)
                            .connectTimeout(connectTimeout)
                            .readTimeout(readTimeout)
                            .requestInterceptor(defaultTimeout(readTimeout));
                    LoggingInterceptor.apply(builder, loggingLevel);
                    return ArkJaxRsProxy.create(iface, builder.build());
                } else {
                    SSLContext sslContext = resolveTls(tlsConfigName);
                    ArkClient.Builder builder = ArkClient.builder()
                            .serializer(serializer)
                            .transport(buildJdkTransport(httpVersion, connectTimeout, sslContext))
                            .baseUrl(baseUrl)
                            .connectTimeout(connectTimeout)
                            .readTimeout(readTimeout)
                            .requestInterceptor(defaultTimeout(readTimeout));
                    LoggingInterceptor.apply(builder, loggingLevel);
                    return ArkJaxRsProxy.create(iface, builder.build());
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to create Ark client for " + interfaceName, e);
            }
        };
    }

    private static String resolveBaseUrl(ArkClientNamedConfig config, RegisterArkClient annotation) {
        if (config != null && config.baseUrl().isPresent()) {
            return config.baseUrl().get();
        }
        return annotation != null ? annotation.baseUrl() : "";
    }

    private static SSLContext resolveTls(String tlsConfigName) {
        if (StringUtils.isEmpty(tlsConfigName)) return null;
        TlsResolver resolver = Arc.container().instance(TlsResolver.class).get();
        return resolver.resolve(tlsConfigName);
    }

    private static ArkJdkHttpTransport buildJdkTransport(String httpVersion, int connectTimeout,
                                                          SSLContext sslContext) {
        HttpClient.Builder httpBuilder = HttpClient.newBuilder()
                .version("HTTP_2".equals(httpVersion)
                        ? HttpClient.Version.HTTP_2
                        : HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(connectTimeout));
        if (sslContext != null) {
            httpBuilder.sslContext(sslContext);
        }
        return new ArkJdkHttpTransport(httpBuilder.build());
    }

    private static ArkVertxMutinyTransport buildMutinyTransport(String httpVersion, int connectTimeout,
                                                                 int readTimeout, String tlsConfigName) {
        Vertx vertx = Arc.container().instance(Vertx.class).get();
        WebClientOptions options = new WebClientOptions()
                .setProtocolVersion("HTTP_2".equals(httpVersion)
                        ? HttpVersion.HTTP_2 : HttpVersion.HTTP_1_1)
                .setConnectTimeout(connectTimeout * 1000)
                .setIdleTimeout(readTimeout);

        if (StringUtils.isNotEmpty(tlsConfigName)) {
            QuarkusVertxTlsResolver vertxTlsResolver =
                    Arc.container().instance(QuarkusVertxTlsResolver.class).get();
            options.setSsl(true)
                    .setTrustOptions(vertxTlsResolver.resolveTrustOptions(tlsConfigName));
            vertxTlsResolver.resolveKeyCertOptions(tlsConfigName)
                    .ifPresent(options::setKeyCertOptions);
        }

        return new ArkVertxMutinyTransport(WebClient.create(vertx, options));
    }

    private static xyz.juandiii.ark.interceptor.RequestInterceptor defaultTimeout(int readTimeout) {
        return ctx -> {
            if (ctx.timeout() == null) {
                ctx.timeout(Duration.ofSeconds(readTimeout));
            }
        };
    }

    private static boolean usesReactiveReturnTypes(Class<?> iface) {
        for (Method method : iface.getMethods()) {
            String name = method.getReturnType().getName();
            if (name.equals("io.smallrye.mutiny.Uni")
                    || name.equals("io.smallrye.mutiny.Multi")) {
                return true;
            }
        }
        return false;
    }

    private static boolean usesAsyncReturnTypes(Class<?> iface) {
        for (Method method : iface.getMethods()) {
            if (method.getReturnType() == java.util.concurrent.CompletableFuture.class) {
                return true;
            }
        }
        return false;
    }
}
