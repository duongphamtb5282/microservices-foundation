package com.pacific.auth.config.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for JWT validation. Provides secure defaults for JWT issuer validation.
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth-service.security.jwt.validation")
public class JwtValidationProperties {

  /**
   * List of allowed JWT issuers for exact match validation. More secure than using contains()
   * checks.
   */
  private List<String> allowedIssuers = new ArrayList<>(Arrays.asList("auth-service"));

  /**
   * Require exact issuer match (recommended for production). If false, falls back to contains()
   * check (less secure).
   */
  private boolean requireExactMatch = true;

  /** Allow localhost in issuer for development (should be false in production). */
  private boolean allowLocalhostIssuer = false;

  /** Check if an issuer is allowed based on configuration */
  public boolean isAllowedIssuer(String issuer) {
    if (issuer == null) {
      return false;
    }

    // First, check exact match
    if (allowedIssuers.contains(issuer)) {
      return true;
    }

    // If exact match is required, stop here
    if (requireExactMatch) {
      // Allow localhost only in development
      if (allowLocalhostIssuer && issuer.contains("localhost")) {
        return true;
      }
      return false;
    }

    // Fallback to contains check (less secure, only if requireExactMatch=false)
    for (String allowedIssuer : allowedIssuers) {
      if (issuer.contains(allowedIssuer)) {
        return true;
      }
    }

    return false;
  }
}
