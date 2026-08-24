package com.pacific.core.filter;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

  public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  public static final String CORRELATION_ID_MDC_KEY = "correlationId";
  public static final String CORRELATION_ID_ATTR = "correlationId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // Robust extraction of correlation ID from header
    String correlationId = extractOrGenerateCorrelationId(request);

    // Store in MDC for logging (defensive - never null)
    MDC.put(CORRELATION_ID_MDC_KEY, correlationId != null ? correlationId : "unknown");

    // Store in request attributes for metrics (CRITICAL for Actuator)
    request.setAttribute(CORRELATION_ID_ATTR, correlationId);

    // Add to response header for client visibility (always return one)
    response.setHeader(CORRELATION_ID_HEADER, correlationId);

    // Wrap request/response for content metrics
    ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
    ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

    try {
      filterChain.doFilter(wrappedRequest, wrappedResponse);
      wrappedResponse.copyBodyToResponse();
    } finally {
      // Always clean up MDC to prevent memory leaks
      MDC.remove(CORRELATION_ID_MDC_KEY);
    }
  }

  /**
   * Extract correlation ID from request header or generate a new UUID-based one This method is
   * defensive against null/empty/malformed headers
   */
  private String extractOrGenerateCorrelationId(HttpServletRequest request) {
    // Defensive header extraction - handles null cases. getHeader cannot throw (ADR-0012), so the
    // former catch-all fallback is gone.
    String headerValue = request.getHeader(CORRELATION_ID_HEADER);

    if (headerValue != null) {
      String trimmedValue = headerValue.trim();
      // Accept only well-formed, reasonably-sized correlation IDs; otherwise generate a new one
      if (!trimmedValue.isEmpty() && trimmedValue.length() >= 5 && trimmedValue.length() <= 100) {
        return trimmedValue;
      }
    }
    return UUID.randomUUID().toString();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // Apply to all endpoints except actuator health/metrics
    String path = request.getRequestURI();
    return path != null
        && (path.startsWith("/actuator/health")
            || path.startsWith("/actuator/prometheus")
            || path.startsWith("/actuator/metrics"));
  }

  // Optional: Add logging capability (requires logger injection)
  // private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
}
