package com.pacific.auth.modules.authentication.security.jwt.common;

import java.util.List;
import java.util.Map;

/**
 * Interface for JWT token validation services. Provides a common contract for token validation
 * across different implementations.
 */
public interface JwtTokenValidationService {

  /**
   * Validate JWT token and return validation result.
   *
   * @param token the JWT token to validate
   * @return JwtValidationResult containing validation status and user information
   */
  JwtValidationResult validateToken(String token);

  /**
   * Extract username from token.
   *
   * @param token the JWT token
   * @return username
   * @throws IllegalArgumentException if token is invalid
   */
  String extractUsername(String token);

  /**
   * Extract user details from token.
   *
   * @param token the JWT token
   * @return Map of user details
   * @throws IllegalArgumentException if token is invalid
   */
  Map<String, Object> extractUserDetails(String token);

  /**
   * Extract roles from token.
   *
   * @param token the JWT token
   * @return List of roles
   * @throws IllegalArgumentException if token is invalid
   */
  List<String> extractRoles(String token);

  /**
   * Check if token has a specific role.
   *
   * @param token the JWT token
   * @param role the role to check
   * @return true if token has the role
   */
  boolean hasRole(String token, String role);

  /**
   * Check if token has any of the specified roles.
   *
   * @param token the JWT token
   * @param roles the roles to check
   * @return true if token has any of the roles
   */
  boolean hasAnyRole(String token, String... roles);
}
