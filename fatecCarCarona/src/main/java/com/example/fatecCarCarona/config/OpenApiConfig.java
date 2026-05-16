package com.example.fatecCarCarona.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "FatecRide API",
        version = "v1.0",
        description = "Documentação oficial da API FatecRide, incluindo o fluxo automático de caronas e SSE.",
        contact = @Contact(name = "Equipe FatecRide"),
        license = @License(name = "Uso acadêmico")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Servidor local")
    },
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig {
    // Configuração central do Swagger/OpenAPI.
}

