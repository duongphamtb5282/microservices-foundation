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
    try {
      // Defensive header extraction - handles null cases
      String headerValue = request.getHeader(CORRELATION_ID_HEADER);

      // Check for null, empty, or whitespace-only values
      if (headerValue == null || headerValue.trim().isEmpty()) {
        // Generate new UUID-based correlation ID
        String generatedId = UUID.randomUUID().toString();
        // Log the generation for debugging (optional)
        // log.debug("Generated new correlation ID: {}", generatedId);
        return generatedId;
      }

      // Validate that it's not just whitespace
      String trimmedValue = headerValue.trim();
      if (trimmedValue.isEmpty()) {
        // Treat as missing and generate new
        return UUID.randomUUID().toString();
      }

      // Additional validation: ensure it's a reasonable length and format
      // (Optional: you can add regex validation for UUID format if needed)
      if (trimmedValue.length() < 5 || trimmedValue.length() > 100) {
        // Log warning for suspicious correlation ID
        // log.warn("Suspicious correlation ID length: {}, generating new one",
        // trimmedValue.length());
        return UUID.randomUUID().toString();
      }

      // Return the provided (trimmed) correlation ID
      return trimmedValue;

    } catch (Exception e) {
      // Fallback in case of any unexpected issues
      // log.error("Error extracting correlation ID, generating new one", e);
      return UUID.randomUUID().toString();
    }
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
