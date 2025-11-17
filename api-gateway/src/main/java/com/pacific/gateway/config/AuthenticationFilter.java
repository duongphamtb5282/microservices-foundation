package com.pacific.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Gateway filter for JWT authentication.
 * Validates tokens with the Auth Service before allowing requests to proceed.
 */
@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final WebClient webClient;

    @Value("${gateway.filters.authentication.token-validation-url:http://localhost:8082/api/v1/auth/validate}")
    private String tokenValidationUrl;

    @Value("${gateway.filters.authentication.exclude-paths:/api/v1/auth/login,/api/v1/auth/register,/actuator/**}")
    private List<String> excludePaths;

    public AuthenticationFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClient = webClientBuilder.build();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();

            // Check if path should be excluded from authentication
            if (shouldExclude(path)) {
                log.debug("Skipping authentication for excluded path: {}", path);
                return chain.filter(exchange);
            }

            // Extract Authorization header
            String authHeader = exchange.getRequest().getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header for path: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7); // Remove "Bearer " prefix

            // Validate token with Auth Service
            return webClient.post()
                .uri(tokenValidationUrl)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue("{\"token\":\"" + token + "\"}")
                .retrieve()
                .bodyToMono(TokenValidationResponse.class)
                .flatMap(response -> {
                    if (response.isValid()) {
                        // Add user info to request headers for downstream services
                        exchange.getRequest().mutate()
                            .header("X-User-Id", response.getUserId())
                            .header("X-Username", response.getUsername())
                            .header("X-Correlation-Id", response.getCorrelationId());

                        log.debug("Token validated successfully for user: {}", response.getUserId());
                        return chain.filter(exchange);
                    } else {
                        log.warn("Token validation failed for path: {}", path);
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("Error validating token with Auth Service: {}", ex.getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Unexpected error in authentication filter: {}", ex.getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    return exchange.getResponse().setComplete();
                });
        };
    }

    private boolean shouldExclude(String path) {
        return excludePaths.stream().anyMatch(excludePath -> {
            if (excludePath.endsWith("/**")) {
                return path.startsWith(excludePath.substring(0, excludePath.length() - 3));
            }
            return path.equals(excludePath);
        });
    }

    public static class Config {
        // Configuration properties if needed
    }

    /**
     * Response from Auth Service token validation.
     */
    public static class TokenValidationResponse {
        private boolean valid;
        private String userId;
        private String username;
        private String correlationId;

        public TokenValidationResponse() {}

        public TokenValidationResponse(boolean valid, String userId, String username, String correlationId) {
            this.valid = valid;
            this.userId = userId;
            this.username = username;
            this.correlationId = correlationId;
        }

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    }
}
