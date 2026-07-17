package xyz.juandiii.ark.quarkus;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.juandiii.ark.core.ArkClient;
import xyz.juandiii.ark.jackson.classic.JacksonClassicSerializer;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.http.HttpTransport;
import xyz.juandiii.ark.transport.jdk.ArkJdkSyncTransport;

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
        return new ArkJdkSyncTransport(HttpClient.newBuilder().build());
    }

    @Produces
    @Dependent
    @DefaultBean
    public ArkClient.Builder arkClientBuilder(JsonSerializer serializer, HttpTransport transport) {
        return ArkClient.builder()
                .serializer(serializer)
                .transport(transport);
    }
}
