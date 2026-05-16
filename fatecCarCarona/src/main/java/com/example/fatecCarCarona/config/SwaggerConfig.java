package com.example.fatecCarCarona.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**@Configuration
@OpenAPIDefinition(
    info = @Info(title = "API Fatec Car Carona", version = "v1"),
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)*/

/**
 * Classe legada mantida por compatibilidade.
 * A configuração oficial do OpenAPI está em {@link OpenApiConfig}.
 */

public class SwaggerConfig {
    // ...existing code...
}

