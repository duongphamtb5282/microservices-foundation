package com.pacific.core.cors;

import java.util.Arrays;
import java.util.List;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Centralized CORS configuration properties. Eliminates duplication between SecurityConfig and
 * CorsConfiguration filter.
 *
 * <p>This class is designed to be configurable and inheritable by services. Services can override
 * these properties in their application.yml files.
 */
@Data
@ConfigurationProperties(prefix = "security.cors")
public class CorsProperties {

  /** Allowed origins (comma-separated) */
  private String allowedOrigins = "*";

  /** Allowed HTTP methods (comma-separated) */
  private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";

  /** Allowed headers (comma-separated) */
  private String allowedHeaders = "*";

  /** Whether to allow credentials */
  private boolean allowCredentials = true;

  /** Max age for preflight requests in seconds */
  private long maxAge = 3600L;

  /** Get allowed origins as List */
  public List<String> getAllowedOriginsList() {
    return Arrays.asList(allowedOrigins.split(","));
  }

  /** Get allowed methods as List */
  public List<String> getAllowedMethodsList() {
    return Arrays.asList(allowedMethods.split(","));
  }

  /** Get allowed headers as List */
  public List<String> getAllowedHeadersList() {
    return Arrays.asList(allowedHeaders.split(","));
  }
}
