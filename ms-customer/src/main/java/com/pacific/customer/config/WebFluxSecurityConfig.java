package com.pacific.customer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration for ms-customer service. Configures OAuth2 resource server with selective
 * endpoint access (Swagger UI and actuator are public; everything else requires a valid JWT).
 *
 * <p>Active in ALL environments (S-01): previously restricted to {@code dev}/{@code staging}, which
 * left production without any authentication enforcement. The JWT issuer/jwk-set URI is resolved
 * from {@code spring.security.oauth2.resourceserver.jwt.*} properties per profile (see
 * application-{dev,stg,prod}.yml), so no environment-specific values are hardcoded here.
 *
 * <p>Named {@code WebFluxSecurityConfig} (not {@code SecurityConfig}) to avoid a bean-name clash
 * with {@code com.pacific.core.security.SecurityConfig}, which {@code BackendCoreAutoConfiguration}'s
 * {@code @ComponentScan("com.pacific.core")} pulls into this context — two same-named
 * {@code @Configuration} classes would fail startup with a ConflictingBeanDefinitionException.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class WebFluxSecurityConfig {

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    http.csrf()
        .disable()
        .authorizeExchange(
            exchange ->
                exchange
                    // Allow unauthenticated access to Swagger UI and API docs
                    .pathMatchers("/webjars/swagger-ui/**")
                    .permitAll()
                    .pathMatchers("/v3/api-docs/**")
                    .permitAll()
                    .pathMatchers("/swagger-ui/**")
                    .permitAll()

                    // Allow unauthenticated access to actuator endpoints
                    .pathMatchers("/actuator/**")
                    .permitAll()

                    // All other requests require authentication
                    .anyExchange()
                    .authenticated())
        .oauth2ResourceServer(ServerHttpSecurity.OAuth2ResourceServerSpec::jwt);

    return http.build();
  }
}
