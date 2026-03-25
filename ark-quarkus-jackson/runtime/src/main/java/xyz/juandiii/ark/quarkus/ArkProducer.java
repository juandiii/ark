package xyz.juandiii.ark.quarkus;

import io.quarkus.arc.DefaultBean;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.juandiii.ark.ArkClient;
import xyz.juandiii.ark.JacksonClassicSerializer;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.http.HttpTransport;
import xyz.juandiii.ark.mutiny.MutinyArkClient;
import xyz.juandiii.ark.mutiny.http.MutinyHttpTransport;
import xyz.juandiii.ark.transport.jdk.ArkJdkHttpTransport;
import xyz.juandiii.ark.transport.vertx.mutiny.ArkVertxMutinyTransport;

import java.net.http.HttpClient;

/**
 * CDI producer for Ark HTTP client beans in Quarkus.
 *
 * @author Juan Diego Lopez V.
 */
@ApplicationScoped
public class ArkProducer {

    @Produces
    @Singleton
    @DefaultBean
    public JsonSerializer jsonSerializer(ObjectMapper objectMapper) {
        return new JacksonClassicSerializer(objectMapper);
    }

    @Produces
    @Singleton
    @DefaultBean
    public HttpTransport httpTransport() {
        return new ArkJdkHttpTransport(HttpClient.newBuilder().build());
    }

    @Produces
    @Singleton
    @DefaultBean
    public MutinyHttpTransport mutinyHttpTransport(Vertx vertx) {
        return new ArkVertxMutinyTransport(WebClient.create(vertx));
    }

    @Produces
    @Dependent
    @DefaultBean
    public ArkClient.Builder arkClientBuilder(JsonSerializer serializer, HttpTransport transport) {
        return ArkClient.builder()
                .serializer(serializer)
                .transport(transport);
    }

    @Produces
    @Dependent
    @DefaultBean
    public MutinyArkClient.Builder mutinyArkClientBuilder(JsonSerializer serializer,
                                                           MutinyHttpTransport transport) {
        return MutinyArkClient.builder()
                .serializer(serializer)
                .transport(transport);
    }
}
