package com.foodapp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Without this, Swagger UI has no "Authorize" button, so you can't test
 * JWT-protected endpoints (like placing an order) directly from the browser -
 * you'd have to copy the token into Postman instead.
 *
 * This registers a Bearer auth scheme named "bearerAuth" and applies it
 * globally, so every endpoint in Swagger UI gets a padlock icon. Click
 * "Authorize" once, paste your JWT (no need to type "Bearer " - Swagger adds
 * that prefix for you), and every subsequent "Try it out" call in the UI
 * sends that token automatically.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI foodOrderingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Online Food Ordering System API")
                        .description("Spring Boot + PostgreSQL backend: restaurants, menus, orders with a status state machine")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(SCHEME_NAME, new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                // applies the bearerAuth requirement to every operation by default
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
    }
}
