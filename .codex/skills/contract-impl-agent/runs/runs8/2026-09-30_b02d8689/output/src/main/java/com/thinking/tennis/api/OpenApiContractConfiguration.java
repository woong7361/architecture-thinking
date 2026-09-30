package com.thinking.tennis.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiContractConfiguration {
    @Bean
    public OpenAPI tennisAlertOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tennis court availability alert API")
                        .version("1.0.0"))
                .servers(java.util.List.of(new Server()
                        .url("https://api.tennis-alert.example.com/v1")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")));
    }
}
