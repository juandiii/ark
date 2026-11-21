package xyz.juandiii.ark.spring.clients;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import xyz.juandiii.ark.spring.EnableArkClients;

@Configuration
@EnableArkClients
public class ClientsConfig {

    @Bean
    ObjectMapper objectMapper() {
        return JsonMapper.builder().build();
    }
}
