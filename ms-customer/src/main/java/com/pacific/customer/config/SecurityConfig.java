package com.pacific.customer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration for ms-customer service.
 * Configures OAuth2 resource server with selective endpoint access for development.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Profile({"dev", "staging"}) // Only apply in development/staging
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf().disable()
            .authorizeExchange(exchange -> exchange
                // Allow unauthenticated access to Swagger UI and API docs
                .pathMatchers("/webjars/swagger-ui/**").permitAll()
                .pathMatchers("/v3/api-docs/**").permitAll()
                .pathMatchers("/swagger-ui/**").permitAll()

                // Allow unauthenticated access to actuator endpoints
                .pathMatchers("/actuator/**").permitAll()

                // All other requests require authentication
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(ServerHttpSecurity.OAuth2ResourceServerSpec::jwt);

        return http.build();
    }
}
