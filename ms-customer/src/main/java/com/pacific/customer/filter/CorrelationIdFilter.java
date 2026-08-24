package com.pacific.customer.filter;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Reactive WebFilter for correlation ID handling in WebFlux. Replaces the servlet-based filter for
 * reactive applications.
 */
@Slf4j
@Component
public class CorrelationIdFilter implements WebFilter {

  public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  public static final String CORRELATION_ID_MDC_KEY = "correlationId";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    // Extract or generate correlation ID
    String correlationId = extractOrGenerateCorrelationId(exchange);

    // Add correlation ID to MDC for logging
    MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

    // Add correlation ID to response headers
    exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

    // Store in exchange attributes for use in controllers/services
    exchange.getAttributes().put(CORRELATION_ID_MDC_KEY, correlationId);

    return chain
        .filter(exchange)
        .doFinally(
            signalType -> {
              // Always clean up MDC to prevent memory leaks
              MDC.remove(CORRELATION_ID_MDC_KEY);
            });
  }

  /** Extract correlation ID from request header or generate a new UUID-based one */
  private String extractOrGenerateCorrelationId(ServerWebExchange exchange) {
    // getFirst cannot throw (ADR-0012), so the former catch-all fallback is gone.
    String headerValue = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);

    if (headerValue != null) {
      String trimmedValue = headerValue.trim();
      // Accept only well-formed, reasonably-sized correlation IDs; otherwise generate a new one
      if (!trimmedValue.isEmpty() && trimmedValue.length() >= 5 && trimmedValue.length() <= 100) {
        return trimmedValue;
      }
    }
    return UUID.randomUUID().toString();
  }
}
