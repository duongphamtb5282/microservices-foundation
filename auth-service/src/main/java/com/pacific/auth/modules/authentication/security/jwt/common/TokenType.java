package com.pacific.auth.modules.authentication.security.jwt.common;

/** Enum representing different types of JWT tokens. */
public enum TokenType {
  CUSTOM("Custom JWT"),
  KEYCLOAK("Keycloak JWT"),
  UNKNOWN("Unknown JWT");

  private final String displayName;

  TokenType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  /** Determine token type from token string */
  public static TokenType detectFromToken(String token) {
    if (token == null || token.isEmpty()) {
      return UNKNOWN;
    }

    // Simple heuristics to detect token type
    if (token.contains("realm_access") || token.contains("resource_access")) {
      return KEYCLOAK;
    }

    // Custom tokens are typically shorter and don't contain Keycloak-specific claims
    if (token.length() < 1000) {
      return CUSTOM;
    }

    return UNKNOWN;
  }
}
