package com.eventpro.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI eventProOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("EventPro API")
                .description("Billetterie en ligne : evenements, reservations, paiements, remboursements et controle des billets")
                .version("1.0.0"));
    }
}