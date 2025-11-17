package com.pacific.core.cors;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Enhanced CORS filter with configurable settings. Provides flexible CORS configuration for
 * microservices. Now uses shared CORS configuration to eliminate duplication.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@RequiredArgsConstructor
public class CorsConfiguration implements Filter {

  private final CorsConfigurationHelper corsConfigurationHelper;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    log.info("🔧 Initializing CORS filter with shared configuration");
  }

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {

    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    // Apply CORS headers using shared configuration
    corsConfigurationHelper.applyCorsHeaders(request, response);

    // Handle preflight requests
    if (corsConfigurationHelper.isPreflightRequest(request)) {
      corsConfigurationHelper.handlePreflightRequest(response);
      return;
    }

    filterChain.doFilter(servletRequest, servletResponse);
  }

  @Override
  public void destroy() {
    log.info("🔧 CORS filter destroyed");
  }
}
