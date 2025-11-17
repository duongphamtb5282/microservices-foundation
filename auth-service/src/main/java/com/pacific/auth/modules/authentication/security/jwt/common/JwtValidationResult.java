package com.pacific.auth.modules.authentication.security.jwt.common;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Result of JWT token validation. Contains validation status, user information, and extracted
 * claims.
 */
@Data
@Builder
public class JwtValidationResult {

  private boolean valid;
  private String username;
  private String email;
  private String firstName;
  private String lastName;
  private List<String> roles;
  private String issuer;
  private Instant issuedAt;
  private Instant expiresAt;
  private Map<String, Object> claims;
  private String errorMessage;
  private String tokenType;

  /** Create a successful validation result */
  public static JwtValidationResult success(
      String username,
      String email,
      String firstName,
      String lastName,
      List<String> roles,
      String issuer,
      Instant issuedAt,
      Instant expiresAt,
      Map<String, Object> claims,
      String tokenType) {
    return JwtValidationResult.builder()
        .valid(true)
        .username(username)
        .email(email)
        .firstName(firstName)
        .lastName(lastName)
        .roles(roles)
        .issuer(issuer)
        .issuedAt(issuedAt)
        .expiresAt(expiresAt)
        .claims(claims)
        .tokenType(tokenType)
        .build();
  }

  /** Create a failed validation result */
  public static JwtValidationResult failure(String errorMessage) {
    return JwtValidationResult.builder().valid(false).errorMessage(errorMessage).build();
  }

  /** Check if the token is expired */
  public boolean isExpired() {
    return expiresAt != null && expiresAt.isBefore(Instant.now());
  }

  /** Check if the token has a specific role */
  public boolean hasRole(String role) {
    return roles != null && (roles.contains(role) || roles.contains("ROLE_" + role.toUpperCase()));
  }

  /** Check if the token has any of the specified roles */
  public boolean hasAnyRole(String... roles) {
    if (this.roles == null) {
      return false;
    }

    for (String role : roles) {
      if (hasRole(role)) {
        return true;
      }
    }
    return false;
  }

  /** Get a claim value by key */
  public Object getClaim(String key) {
    return claims != null ? claims.get(key) : null;
  }

  /** Get a claim value as string */
  public String getClaimAsString(String key) {
    Object value = getClaim(key);
    return value != null ? value.toString() : null;
  }
}
