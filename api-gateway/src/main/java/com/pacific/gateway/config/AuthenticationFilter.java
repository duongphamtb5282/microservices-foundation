package com.pacific.gateway.config;

import com.pacific.gateway.config.JwtLocalValidator.TokenValidationResult;
import com.pacific.gateway.exception.JwtValidationException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway filter for JWT authentication. Verifies Bearer tokens locally (RS256 signature against
 * the Keycloak JWKS, issuer and expiry checks) before allowing requests to proceed. No round-trip
 * to the Auth Service is performed.
 */
@Component
@Slf4j
public class AuthenticationFilter
    extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

  private static final byte[] UNAUTHORIZED_BODY =
      "{\"error\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
  private static final byte[] INTERNAL_ERROR_BODY =
      "{\"error\":\"Internal Server Error\"}".getBytes(StandardCharsets.UTF_8);

  private final JwtLocalValidator jwtLocalValidator;

  @Value(
      "${gateway.filters.authentication.exclude-paths:/api/v1/auth/login,/api/v1/auth/register,/actuator/**}")
  private List<String> excludePaths;

  public AuthenticationFilter(JwtLocalValidator jwtLocalValidator) {
    super(Config.class);
    this.jwtLocalValidator = jwtLocalValidator;
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
      String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        log.warn("Missing or invalid Authorization header for path: {}", path);
        return unauthorized(exchange);
      }

      String token = authHeader.substring(7); // Remove "Bearer " prefix

      // Validate token locally (RS256 signature, issuer and expiry)
      try {
        TokenValidationResult result = jwtLocalValidator.validateToken(token);

        // Add user info to request headers for downstream services
        ServerHttpRequest.Builder requestBuilder =
            exchange
                .getRequest()
                .mutate()
                .header("X-User-Id", result.userId())
                .header("X-Username", result.username());
        if (!result.roles().isEmpty()) {
          requestBuilder.header("X-User-Roles", String.join(",", result.roles()));
        }
        ServerHttpRequest mutatedRequest = requestBuilder.build();

        log.debug("Token validated successfully for user: {}", result.username());
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
      } catch (JwtValidationException ex) {
        log.warn("Token validation failed for path: {}: {}", path, ex.getMessage());
        return unauthorized(exchange);
      } catch (Exception ex) {
        log.error(
            "Unexpected error in authentication filter for path: {}: {}", path, ex.getMessage());
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return writeBody(exchange, INTERNAL_ERROR_BODY);
      }
    };
  }

  private boolean shouldExclude(String path) {
    return excludePaths.stream()
        .anyMatch(
            excludePath -> {
              if (excludePath.endsWith("/**")) {
                return path.startsWith(excludePath.substring(0, excludePath.length() - 3));
              }
              return path.equals(excludePath);
            });
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    return writeBody(exchange, UNAUTHORIZED_BODY);
  }

  private Mono<Void> writeBody(ServerWebExchange exchange, byte[] body) {
    return exchange
        .getResponse()
        .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
  }

  public static class Config {
    // Configuration properties if needed
  }
}
