package xyz.juandiii.ark.spring.webflux.clients;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.reactor.http.ReactorHttpTransport;
import xyz.juandiii.ark.spring.webflux.EnableArkWebFluxClients;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Configuration
@EnableArkWebFluxClients
public class ClientsConfig {

    public static final AtomicReference<Duration> lastTimeout = new AtomicReference<>();

    @Bean
    ObjectMapper objectMapper() {
        return JsonMapper.builder().build();
    }

    /** Replaces the default transport and records the per-request timeout it receives. */
    @Bean
    ReactorHttpTransport reactorHttpTransport() {
        return (method, uri, headers, body, timeout) -> {
            lastTimeout.set(timeout);
            return Mono.just(new RawResponse(200, Map.of("Content-Type", List.of("text/plain")), "ok"));
        };
    }
}
