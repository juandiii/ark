package xyz.juandiii.ark.quarkus.vertx;

import io.quarkus.arc.DefaultBean;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.mutiny.MutinyArkClient;
import xyz.juandiii.ark.mutiny.http.MutinyHttpTransport;
import xyz.juandiii.ark.transport.vertx.mutiny.ArkVertxMutinyTransport;

/**
 * CDI producer for the Ark Vert.x Mutiny transport add-on beans in Quarkus.
 *
 * @author Juan Diego Lopez V.
 */
@ApplicationScoped
public class ArkVertxProducer {

    @Produces
    @Singleton
    @DefaultBean
    public MutinyHttpTransport mutinyHttpTransport(Vertx vertx) {
        return new ArkVertxMutinyTransport(WebClient.create(vertx));
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
