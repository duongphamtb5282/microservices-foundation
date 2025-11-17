package com.pacific.customer.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for ms-customer service.
 * Provides Swagger UI documentation with JWT Bearer authentication.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Customer Service API",
        version = "1.0.0",
        description = "REST API for customer management in the microservices architecture",
        contact = @Contact(
            name = "Development Team",
            email = "dev@pacific.com"
        ),
        license = @License(
            name = "MIT",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:8084",
            description = "Development server"
        ),
        @Server(
            url = "https://api.pacific.com/customer",
            description = "Production server"
        )
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig {
}
