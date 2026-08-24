package com.pacific.order.infrastructure.client;

import com.pacific.core.messaging.circuitbreaker.CircuitBreakerService;
import com.pacific.order.infrastructure.client.dto.ValidateApiKeyRequest;
import com.pacific.order.infrastructure.client.dto.ValidateTokenRequest;
import com.pacific.order.infrastructure.client.dto.ValidateTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Boundary wrapper for the order→auth Feign client (ADR-0010). Every call runs through
 * {@link CircuitBreakerService#execute(String, java.util.function.Supplier)} with the
 * "auth-service" breaker, so a slow or unavailable auth-service trips the breaker instead of
 * exhausting order-service threads call after call.
 *
 * <p>Failure semantics: the validation fails <b>closed</b> — a degraded response (invalid token /
 * API key) is returned when auth-service is unreachable or the breaker is open. Identity cannot be
 * verified, so the request must not proceed; the breaker metrics/alerts (CircuitBreakerAlertService,
 * BusinessHealthIndicator) carry the cause.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthValidationService {

  private final AuthServiceClient authClient;
  private final CircuitBreakerService circuitBreakerService;

  /**
   * Validate a JWT token via auth-service; returns a closed (invalid) response on failure.
   *
   * <p>The controllers hand over the raw {@code Authorization} header value ("Bearer eyJ...") — the
   * Feign contract sends the bare JWT in the body, so the prefix is stripped here. Without this,
   * auth's JWT decoder chokes on "Bearer eyJ..." and every valid token validates as false
   * (2026-08-25: reproduced end-to-end — bare token valid:true, prefixed valid:false).
   */
  public ValidateTokenResponse validateToken(String token) {
    try {
      return circuitBreakerService.execute(
          "auth-service",
          () -> authClient.validateToken(new ValidateTokenRequest(stripBearerPrefix(token))));
    } catch (Exception e) {
      // Boundary catch (ADR-0010/0012): any failure of the external call — FeignException,
      // CallNotPermittedException, timeouts — degrades to "invalid" instead of a 500.
      log.warn("Auth-service unavailable, failing token validation closed: {}", e.getMessage());
      return ValidateTokenResponse.builder()
          .valid(false)
          .message("Authentication service unavailable")
          .build();
    }
  }

  /** "Bearer eyJ..." (raw Authorization header) -> "eyJ...". Null-safe; tolerates extra spaces. */
  private static String stripBearerPrefix(String token) {
    if (token == null) {
      return null;
    }
    String trimmed = token.trim();
    if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
      return trimmed.substring(7).trim();
    }
    return trimmed;
  }

  /** Validate an API key via auth-service; returns false (closed) on failure. */
  public boolean validateApiKey(String apiKey) {
    try {
      return circuitBreakerService.execute(
          "auth-service", () -> authClient.validateApiKey(new ValidateApiKeyRequest(apiKey)));
    } catch (Exception e) {
      // Boundary catch: same fail-closed semantics as validateToken.
      log.warn("Auth-service unavailable, failing API-key validation closed: {}", e.getMessage());
      return false;
    }
  }
}
