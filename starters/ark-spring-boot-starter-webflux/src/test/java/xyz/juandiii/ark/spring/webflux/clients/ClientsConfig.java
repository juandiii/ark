package xyz.juandiii.ark.spring.webflux.clients;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import xyz.juandiii.ark.spring.webflux.EnableArkWebFluxClients;

@Configuration
@EnableArkWebFluxClients
public class ClientsConfig {

    @Bean
    ObjectMapper objectMapper() {
        return JsonMapper.builder().build();
    }
}
