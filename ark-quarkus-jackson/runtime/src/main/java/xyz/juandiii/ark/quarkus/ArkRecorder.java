package xyz.juandiii.ark.quarkus;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import xyz.juandiii.ark.ArkClient;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.async.AsyncArkClient;
import xyz.juandiii.ark.interceptor.LoggingInterceptor;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.proxy.InterceptorResolver;
import xyz.juandiii.ark.mutiny.MutinyArkClient;
import xyz.juandiii.ark.proxy.HttpVersion;
import xyz.juandiii.ark.ssl.InsecureSslContext;
import xyz.juandiii.ark.proxy.PropertyResolver;
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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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

                String key = StringUtils.isNotEmpty(configKey) ? configKey : interfaceName;
                ArkClientNamedConfig config = clientsConfig.client().get(key);
                RegisterArkClient annotation = iface.getAnnotation(RegisterArkClient.class);

                ResolvedConfig resolved = resolveConfig(key, config, annotation, clientsConfig.loggingLevel());
                return buildProxy(iface, serializer, resolved);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to create Ark client for " + interfaceName, e);
            }
        };
    }

    private record ResolvedConfig(String clientName, String baseUrl, HttpVersion httpVersion,
                                   int connectTimeout, int readTimeout, String tlsConfigName,
                                   boolean trustAll, Map<String, String> headers,
                                   Class<?>[] interceptorClasses,
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
                config != null ? config.headers() : Map.of(),
                annotation != null ? annotation.interceptors() : new Class<?>[0],
                loggingLevel
        );
    }

    private static Object buildProxy(Class<?> iface, JsonSerializer serializer, ResolvedConfig rc) {
        if (usesReactiveReturnTypes(iface)) {
            MutinyArkClient.Builder builder = MutinyArkClient.builder()
                    .serializer(serializer)
                    .transport(buildMutinyTransport(rc))
                    .baseUrl(rc.baseUrl())
                    .httpVersion(rc.httpVersion())
                    .connectTimeout(rc.connectTimeout())
                    .readTimeout(rc.readTimeout());
            applyInterceptors(builder, rc);
            return ArkJaxRsProxy.create(iface, builder.build());
        } else if (usesAsyncReturnTypes(iface)) {
            SSLContext sslContext = resolveSslContext(rc.clientName(), rc.tlsConfigName(), rc.trustAll());
            AsyncArkClient.Builder builder = AsyncArkClient.builder()
                    .serializer(serializer)
                    .transport(buildJdkTransport(rc.httpVersion(), rc.connectTimeout(), sslContext))
                    .baseUrl(rc.baseUrl())
                    .httpVersion(rc.httpVersion())
                    .connectTimeout(rc.connectTimeout())
                    .readTimeout(rc.readTimeout())
                    .requestInterceptor(defaultTimeout(rc.readTimeout()));
            applyInterceptors(builder, rc);
            return ArkJaxRsProxy.create(iface, builder.build());
        }
        SSLContext sslContext = resolveSslContext(rc.clientName(), rc.tlsConfigName(), rc.trustAll());
        ArkClient.Builder builder = ArkClient.builder()
                .serializer(serializer)
                .transport(buildJdkTransport(rc.httpVersion(), rc.connectTimeout(), sslContext))
                .baseUrl(rc.baseUrl())
                .httpVersion(rc.httpVersion())
                .connectTimeout(rc.connectTimeout())
                .readTimeout(rc.readTimeout())
                .requestInterceptor(defaultTimeout(rc.readTimeout()));
        applyInterceptors(builder, rc);
        return ArkJaxRsProxy.create(iface, builder.build());
    }

    private static <B extends xyz.juandiii.ark.AbstractArkBuilder<B>> void applyInterceptors(
            B builder, ResolvedConfig rc) {
        InterceptorResolver.applyHeaders(builder, rc.headers());
        InterceptorResolver.applyInterceptors(builder, rc.interceptorClasses(),
                clazz -> Arc.container().instance(clazz).get());
        LoggingInterceptor.apply(builder, rc.loggingLevel());
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

    private static SSLContext resolveSslContext(String clientName, String tlsConfigName, boolean trustAll) {
        if (trustAll) return InsecureSslContext.create(clientName);
        if (StringUtils.isEmpty(tlsConfigName)) return null;
        TlsResolver resolver = Arc.container().instance(TlsResolver.class).get();
        return resolver.resolve(tlsConfigName);
    }

    private static ArkJdkHttpTransport buildJdkTransport(HttpVersion httpVersion, int connectTimeout,
                                                          SSLContext sslContext) {
        HttpClient.Builder httpBuilder = HttpClient.newBuilder()
                .version(httpVersion == HttpVersion.HTTP_2
                        ? HttpClient.Version.HTTP_2
                        : HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(connectTimeout));
        if (sslContext != null) {
            httpBuilder.sslContext(sslContext);
        }
        return new ArkJdkHttpTransport(httpBuilder.build());
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

    private static RequestInterceptor defaultTimeout(int readTimeout) {
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
            if (method.getReturnType() == CompletableFuture.class) {
                return true;
            }
        }
        return false;
    }
}