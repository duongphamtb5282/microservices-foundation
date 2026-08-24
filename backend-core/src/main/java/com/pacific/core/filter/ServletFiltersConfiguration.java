package com.pacific.core.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Register servlet Filters only when Servlet API is present. Uses reflection to avoid compile-time
 * servlet dependencies when running the application with WebFlux.
 */
@Configuration
public class ServletFiltersConfiguration {

  @Bean
  public Object correlationIdFilterRegistration() {
    try {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      // Check for servlet API
      Class<?> filterClass = Class.forName("jakarta.servlet.Filter", false, cl);

      // Create CorrelationIdFilter instance
      com.pacific.core.filter.CorrelationIdFilter filter =
          new com.pacific.core.filter.CorrelationIdFilter();

      // Try to use Spring Boot's FilterRegistrationBean if available
      try {
        Class<?> frbClass =
            Class.forName("org.springframework.boot.web.servlet.FilterRegistrationBean", false, cl);
        Object frb = frbClass.getConstructor().newInstance();
        // getMethod requires the EXACT declared parameter type (jakarta.servlet.Filter) — the
        // previous Object.class lookup threw NoSuchMethodException here.
        frbClass.getMethod("setFilter", filterClass).invoke(frb, filter);
        frbClass.getMethod("setOrder", int.class).invoke(frb, Ordered.HIGHEST_PRECEDENCE);
        return frb;
      } catch (ClassNotFoundException cnfe) {
        // FilterRegistrationBean not available; return raw filter
        return filter;
      }
    } catch (ClassNotFoundException e) {
      // Servlet API not present — do not register filter
      return null;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to register CorrelationIdFilter reflectively", e);
    }
  }
}
