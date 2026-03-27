package xyz.juandiii.ark.quarkus;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.eclipse.microprofile.config.ConfigProvider;
import xyz.juandiii.ark.ArkClient;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.async.AsyncArkClient;
import xyz.juandiii.ark.interceptor.LoggingInterceptor;
import xyz.juandiii.ark.mutiny.MutinyArkClient;
import xyz.juandiii.ark.proxy.PropertyResolver;
import xyz.juandiii.ark.proxy.jaxrs.ArkJaxRsProxy;
import xyz.juandiii.ark.transport.jdk.ArkJdkHttpTransport;
import xyz.juandiii.ark.transport.vertx.mutiny.ArkVertxMutinyTransport;

import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Quarkus recorder that creates Ark proxy client beans at runtime.
 *
 * @author Juan Diego Lopez V.
 */
@Recorder
public class ArkRecorder {

    public Supplier<?> createArkClient(String interfaceName, String baseUrl,
                                       String httpVersion, int connectTimeout,
                                       int readTimeout) {
        return () -> {
            try {
                String resolvedUrl = PropertyResolver.resolve(baseUrl,
                        key -> ConfigProvider.getConfig()
                                .getOptionalValue(key, String.class).orElse(null));

                Class<?> iface = Thread.currentThread().getContextClassLoader().loadClass(interfaceName);
                JsonSerializer serializer = Arc.container().instance(JsonSerializer.class).get();
                LoggingInterceptor.Level loggingLevel = LoggingInterceptor.parseLevel(
                        ConfigProvider.getConfig()
                                .getOptionalValue("ark.logging.level", String.class).orElse(null));

                if (usesReactiveReturnTypes(iface)) {
                    MutinyArkClient.Builder builder = MutinyArkClient.builder()
                            .serializer(serializer)
                            .transport(buildMutinyTransport(httpVersion, connectTimeout, readTimeout))
                            .baseUrl(resolvedUrl)
                            .connectTimeout(connectTimeout)
                            .readTimeout(readTimeout);
                    LoggingInterceptor.apply(builder, loggingLevel);
                    return ArkJaxRsProxy.create(iface, builder.build());
                } else if (usesAsyncReturnTypes(iface)) {
                    AsyncArkClient.Builder builder = AsyncArkClient.builder()
                            .serializer(serializer)
                            .transport(buildJdkTransport(httpVersion, connectTimeout))
                            .baseUrl(resolvedUrl)
                            .connectTimeout(connectTimeout)
                            .readTimeout(readTimeout)
                            .requestInterceptor(defaultTimeout(readTimeout));
                    LoggingInterceptor.apply(builder, loggingLevel);
                    return ArkJaxRsProxy.create(iface, builder.build());
                } else {
                    ArkClient.Builder builder = ArkClient.builder()
                            .serializer(serializer)
                            .transport(buildJdkTransport(httpVersion, connectTimeout))
                            .baseUrl(resolvedUrl)
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

    private static ArkJdkHttpTransport buildJdkTransport(String httpVersion, int connectTimeout) {
        return new ArkJdkHttpTransport(HttpClient.newBuilder()
                .version("HTTP_2".equals(httpVersion)
                        ? HttpClient.Version.HTTP_2
                        : HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(connectTimeout))
                .build());
    }

    private static ArkVertxMutinyTransport buildMutinyTransport(String httpVersion,
                                                                 int connectTimeout,
                                                                 int readTimeout) {
        Vertx vertx = Arc.container().instance(Vertx.class).get();
        WebClientOptions options = new WebClientOptions()
                .setProtocolVersion("HTTP_2".equals(httpVersion)
                        ? HttpVersion.HTTP_2 : HttpVersion.HTTP_1_1)
                .setConnectTimeout(connectTimeout * 1000)
                .setIdleTimeout(readTimeout);
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
