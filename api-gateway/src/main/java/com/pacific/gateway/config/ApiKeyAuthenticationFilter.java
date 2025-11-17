package com.pacific.gateway.config;

import com.pacific.core.messaging.security.SecurityService;
import lombok.RequiredArgsConstructor;
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
 * Gateway filter for API key authentication (alternative to JWT).
 * Validates API keys for service-to-service communication.
 */
@Component
@Slf4j
public class ApiKeyAuthenticationFilter extends AbstractGatewayFilterFactory<ApiKeyAuthenticationFilter.Config> {

    private final SecurityService securityService;
    private final WebClient webClient;

    @Value("${gateway.filters.api-key.enabled:false}")
    private boolean apiKeyEnabled;

    @Value("${gateway.filters.api-key.header:X-API-Key}")
    private String apiKeyHeader;

    @Value("${gateway.filters.api-key.exclude-paths:/actuator/**}")
    private List<String> excludePaths;

    public ApiKeyAuthenticationFilter(SecurityService securityService, WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.securityService = securityService;
        this.webClient = webClientBuilder.build();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();

            // Check if path should be excluded
            if (shouldExclude(path)) {
                log.debug("Skipping API key authentication for excluded path: {}", path);
                return chain.filter(exchange);
            }

            // Check if API key authentication is enabled
            if (!apiKeyEnabled) {
                log.debug("API key authentication disabled, proceeding without validation");
                return chain.filter(exchange);
            }

            // Extract API key from header
            String apiKey = exchange.getRequest().getHeaders().getFirst(apiKeyHeader);

            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("Missing API key for path: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                byte[] errorBytes = "{\"error\":\"Missing API Key\",\"code\":\"MISSING_API_KEY\"}".getBytes();
                return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(errorBytes))
                );
            }

            // Validate API key
            if (!securityService.validateApiKey(apiKey)) {
                log.warn("Invalid API key for path: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                byte[] errorBytes = "{\"error\":\"Invalid API Key\",\"code\":\"INVALID_API_KEY\"}".getBytes();
                return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(errorBytes))
                );
            }

            // Add API key info to downstream headers
            exchange.getRequest().mutate()
                .header("X-API-Key-Validated", "true")
                .header("X-Service-Authenticated", "api-key");

            log.debug("API key validated successfully for path: {}", path);
            return chain.filter(exchange);
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
}
