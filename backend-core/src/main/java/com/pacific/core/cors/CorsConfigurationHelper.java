package com.pacific.core.cors;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Centralized CORS configuration helper. Provides reusable CORS configuration for both Spring
 * Security and Servlet filters.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CorsConfigurationHelper {

  private final CorsProperties corsProperties;

  /** Create Spring Security CORS configuration source */
  public CorsConfigurationSource createCorsConfigurationSource() {
    log.info("🔧 Creating CORS configuration source");

    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(corsProperties.getAllowedOriginsList());
    configuration.setAllowedMethods(corsProperties.getAllowedMethodsList());
    configuration.setAllowedHeaders(corsProperties.getAllowedHeadersList());
    configuration.setAllowCredentials(corsProperties.isAllowCredentials());
    configuration.setMaxAge(corsProperties.getMaxAge());

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    log.info(
        "✅ CORS configuration source created with origins: {}, methods: {}, headers: {}",
        corsProperties.getAllowedOrigins(),
        corsProperties.getAllowedMethods(),
        corsProperties.getAllowedHeaders());

    return source;
  }

  /** Apply CORS headers to HTTP response (for Servlet filters) */
  public void applyCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
    // Set CORS headers
    response.setHeader("Access-Control-Allow-Origin", corsProperties.getAllowedOrigins());
    response.setHeader("Access-Control-Allow-Methods", corsProperties.getAllowedMethods());
    response.setHeader("Access-Control-Allow-Headers", corsProperties.getAllowedHeaders());
    response.setHeader("Access-Control-Max-Age", String.valueOf(corsProperties.getMaxAge()));

    if (corsProperties.isAllowCredentials()) {
      response.setHeader("Access-Control-Allow-Credentials", "true");
    }
  }

  /** Check if request is a preflight request */
  public boolean isPreflightRequest(HttpServletRequest request) {
    return "OPTIONS".equalsIgnoreCase(request.getMethod());
  }

  /** Handle preflight request */
  public void handlePreflightRequest(HttpServletResponse response) throws IOException {
    response.setStatus(HttpServletResponse.SC_OK);
  }
}
