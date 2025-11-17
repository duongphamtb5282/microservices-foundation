package com.pacific.auth.modules.authentication.security.jwt.common;

import org.springframework.security.core.Authentication;

/**
 * Interface for JWT authentication tokens. Provides common contract for all JWT-based
 * authentication tokens.
 */
public interface JwtAuthenticationToken extends Authentication {

  /**
   * Get the JWT token string
   *
   * @return the JWT token
   */
  String getJwtToken();

  /**
   * Get the token type enum
   *
   * @return the token type
   */
  TokenType getTokenType();

  /**
   * Check if this is a custom JWT token
   *
   * @return true if this is a custom JWT token
   */
  default boolean isCustomJwtToken() {
    return getTokenType() == TokenType.CUSTOM;
  }

  /**
   * Check if this is a Keycloak JWT token
   *
   * @return true if this is a Keycloak JWT token
   */
  default boolean isKeycloakJwtToken() {
    return getTokenType() == TokenType.KEYCLOAK;
  }

  /**
   * Get the token type display name for logging purposes
   *
   * @return the token type display name
   */
  default String getTokenTypeDisplayName() {
    return getTokenType().getDisplayName();
  }
}
