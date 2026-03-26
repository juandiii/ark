package xyz.juandiii.ark.quarkus;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.eclipse.microprofile.config.ConfigProvider;
import xyz.juandiii.ark.Ark;
import xyz.juandiii.ark.ArkClient;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.http.HttpTransport;
import xyz.juandiii.ark.interceptor.LoggingInterceptor;
import xyz.juandiii.ark.mutiny.MutinyArkClient;
import xyz.juandiii.ark.proxy.ArkProxy;
import xyz.juandiii.ark.proxy.PropertyResolver;
import xyz.juandiii.ark.transport.vertx.mutiny.ArkVertxMutinyTransport;

import java.lang.reflect.Method;
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
                LoggingInterceptor.Level loggingLevel = resolveLoggingLevel();

                Object ark;
                if (usesReactiveReturnTypes(iface)) {
                    Vertx vertx = Arc.container().instance(Vertx.class).get();
                    WebClientOptions options = new WebClientOptions()
                            .setProtocolVersion("HTTP_2".equals(httpVersion)
                                    ? HttpVersion.HTTP_2 : HttpVersion.HTTP_1_1)
                            .setConnectTimeout(connectTimeout * 1000)
                            .setIdleTimeout(readTimeout);
                    MutinyArkClient.Builder builder = MutinyArkClient.builder()
                            .serializer(serializer)
                            .transport(new ArkVertxMutinyTransport(WebClient.create(vertx, options)))
                            .baseUrl(resolvedUrl);
                    LoggingInterceptor.apply(builder, loggingLevel);
                    ark = builder.build();
                } else {
                    HttpTransport transport = Arc.container().instance(HttpTransport.class).get();
                    ArkClient.Builder builder = ArkClient.builder()
                            .serializer(serializer)
                            .transport(transport)
                            .baseUrl(resolvedUrl);
                    LoggingInterceptor.apply(builder, loggingLevel);
                    ark = builder.build();
                }

                return ArkProxy.create(iface, ark);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to create Ark client for " + interfaceName, e);
            }
        };
    }

    private static LoggingInterceptor.Level resolveLoggingLevel() {
        String level = ConfigProvider.getConfig()
                .getOptionalValue("ark.logging.level", String.class)
                .orElse("OFF");
        try {
            return LoggingInterceptor.Level.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LoggingInterceptor.Level.OFF;
        }
    }

    private static boolean usesReactiveReturnTypes(Class<?> iface) {
        for (Method method : iface.getDeclaredMethods()) {
            Class<?> returnType = method.getReturnType();
            String name = returnType.getName();
            if (name.equals("io.smallrye.mutiny.Uni")
                    || name.equals("io.smallrye.mutiny.Multi")) {
                return true;
            }
        }
        return false;
    }
}
