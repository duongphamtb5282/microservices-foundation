package com.pacific.auth.modules.authentication.security.jwt.common;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Abstract base class for JWT token validation services. Provides common functionality and defines
 * the contract for token validation.
 */
@Slf4j
public abstract class AbstractJwtValidationService {

  protected final JwtDecoder jwtDecoder;

  protected AbstractJwtValidationService(JwtDecoder jwtDecoder) {
    this.jwtDecoder = jwtDecoder;
  }

  /**
   * Validate JWT token and return validation result. This is the main entry point for token
   * validation.
   *
   * @param token the JWT token to validate
   * @return JwtValidationResult containing validation status and user information
   */
  public abstract JwtValidationResult validateToken(String token);

  /**
   * Extract username from token. Default implementation uses validateToken().
   *
   * @param token the JWT token
   * @return username
   * @throws IllegalArgumentException if token is invalid
   */
  public String extractUsername(String token) {
    JwtValidationResult result = validateToken(token);
    if (result.isValid()) {
      return result.getUsername();
    }
    throw new IllegalArgumentException("Invalid token: " + result.getErrorMessage());
  }

  /**
   * Extract user details from token. Default implementation uses validateToken().
   *
   * @param token the JWT token
   * @return Map of user details
   * @throws IllegalArgumentException if token is invalid
   */
  public Map<String, Object> extractUserDetails(String token) {
    JwtValidationResult result = validateToken(token);
    if (result.isValid()) {
      return result.getClaims();
    }
    throw new IllegalArgumentException("Invalid token: " + result.getErrorMessage());
  }

  /**
   * Extract roles from token. Default implementation uses validateToken().
   *
   * @param token the JWT token
   * @return List of roles
   * @throws IllegalArgumentException if token is invalid
   */
  public List<String> extractRoles(String token) {
    JwtValidationResult result = validateToken(token);
    if (result.isValid()) {
      return result.getRoles();
    }
    throw new IllegalArgumentException("Invalid token: " + result.getErrorMessage());
  }

  /**
   * Check if token has a specific role.
   *
   * @param token the JWT token
   * @param role the role to check
   * @return true if token has the role
   */
  public boolean hasRole(String token, String role) {
    JwtValidationResult result = validateToken(token);
    return result.isValid() && result.hasRole(role);
  }

  /**
   * Check if token has any of the specified roles.
   *
   * @param token the JWT token
   * @param roles the roles to check
   * @return true if token has any of the roles
   */
  public boolean hasAnyRole(String token, String... roles) {
    JwtValidationResult result = validateToken(token);
    return result.isValid() && result.hasAnyRole(roles);
  }

  /**
   * Decode JWT token using the configured decoder.
   *
   * @param token the JWT token
   * @return decoded JWT
   * @throws Exception if decoding fails
   */
  protected Jwt decodeToken(String token) throws Exception {
    return jwtDecoder.decode(token);
  }

  /**
   * Validate token claims specific to the implementation. This method should be overridden by
   * concrete implementations to add specific validation logic.
   *
   * @param jwt the decoded JWT
   * @throws Exception if validation fails
   */
  protected abstract void validateTokenClaims(Jwt jwt) throws Exception;

  /**
   * Extract roles from JWT claims specific to the implementation. This method should be overridden
   * by concrete implementations to extract roles according to their specific claim structure.
   *
   * @param jwt the decoded JWT
   * @return List of roles
   */
  protected abstract List<String> extractRoles(Jwt jwt);

  /**
   * Get the token type for this implementation.
   *
   * @return TokenType enum value
   */
  protected abstract TokenType getTokenType();

  /**
   * Extract first name from JWT claims specific to the implementation.
   *
   * @param jwt the decoded JWT
   * @return first name or null
   */
  protected abstract String extractFirstName(Jwt jwt);

  /**
   * Extract last name from JWT claims specific to the implementation.
   *
   * @param jwt the decoded JWT
   * @return last name or null
   */
  protected abstract String extractLastName(Jwt jwt);

  /**
   * Create a successful validation result with extracted information.
   *
   * @param jwt the decoded JWT
   * @return JwtValidationResult
   */
  protected JwtValidationResult createSuccessResult(Jwt jwt) {
    String username = jwt.getSubject();
    String email = jwt.getClaimAsString("email");
    String firstName = extractFirstName(jwt);
    String lastName = extractLastName(jwt);
    List<String> roles = extractRoles(jwt);
    String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
    Instant issuedAt = jwt.getIssuedAt();
    Instant expiresAt = jwt.getExpiresAt();
    Map<String, Object> claims = jwt.getClaims();

    log.debug("{} validation successful for user: {}", getTokenType().getDisplayName(), username);

    return JwtValidationResult.success(
        username,
        email,
        firstName,
        lastName,
        roles,
        issuer,
        issuedAt,
        expiresAt,
        claims,
        getTokenType().getDisplayName());
  }

  /**
   * Create a failed validation result with error message.
   *
   * @param errorMessage the error message
   * @return JwtValidationResult
   */
  protected JwtValidationResult createFailureResult(String errorMessage) {
    log.warn("{} validation failed: {}", getTokenType().getDisplayName(), errorMessage);
    return JwtValidationResult.failure(
        "Invalid " + getTokenType().getDisplayName() + ": " + errorMessage);
  }
}
