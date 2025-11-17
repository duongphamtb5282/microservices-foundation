package com.pacific.auth.modules.authentication.security.jwt.keycloak;

import com.pacific.auth.modules.authentication.security.jwt.common.AbstractJwtValidationService;
import com.pacific.auth.modules.authentication.security.jwt.common.JwtTokenValidationService;
import com.pacific.auth.modules.authentication.security.jwt.common.JwtValidationResult;
import com.pacific.auth.modules.authentication.security.jwt.common.TokenType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

/**
 * Service for validating Keycloak tokens and extracting user information. Handles token validation,
 * user details extraction, and role information.
 */
@Service
@Slf4j
@ConditionalOnProperty(
    name = "auth-service.security.authentication.keycloak.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class KeycloakTokenValidationService extends AbstractJwtValidationService
    implements JwtTokenValidationService {

  private final KeycloakProperties keycloakProperties;

  public KeycloakTokenValidationService(
      @Qualifier("keycloakJwtDecoder") JwtDecoder jwtDecoder,
      KeycloakProperties keycloakProperties) {
    super(jwtDecoder);
    this.keycloakProperties = keycloakProperties;
  }

  @Override
  public JwtValidationResult validateToken(String token) {
    try {
      log.debug("Validating Keycloak token");
      Jwt jwt = decodeToken(token);
      validateTokenClaims(jwt);
      return createSuccessResult(jwt);
    } catch (Exception e) {
      return createFailureResult(e.getMessage());
    }
  }

  @Override
  protected void validateTokenClaims(Jwt jwt) throws Exception {
    // Check expiration
    if (jwt.getExpiresAt() != null && jwt.getExpiresAt().isBefore(Instant.now())) {
      throw new IllegalArgumentException("Token has expired");
    }

    // Check issuer
    String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
    if (issuer == null || !issuer.equals(keycloakProperties.getIssuerUri())) {
      throw new IllegalArgumentException("Invalid token issuer: " + issuer);
    }

    // Check audience only if configured to do so
    if (keycloakProperties.isVerifyTokenAudience()) {
      if (jwt.getAudience() == null || jwt.getAudience().isEmpty()) {
        throw new IllegalArgumentException("Token missing audience");
      }
    } else {
      log.debug("Skipping audience verification (verify-token-audience=false)");
    }
  }

  @Override
  protected List<String> extractRoles(Jwt jwt) {
    List<String> roles = new java.util.ArrayList<>();

    // Extract realm roles
    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
    if (realmAccess != null && realmAccess.containsKey("roles")) {
      @SuppressWarnings("unchecked")
      List<String> realmRoles = (List<String>) realmAccess.get("roles");
      roles.addAll(realmRoles);
    }

    // Extract client roles
    Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
    if (resourceAccess != null && keycloakProperties.getResource() != null) {
      @SuppressWarnings("unchecked")
      Map<String, Object> clientRoles =
          (Map<String, Object>) resourceAccess.get(keycloakProperties.getResource());
      if (clientRoles != null && clientRoles.containsKey("roles")) {
        @SuppressWarnings("unchecked")
        List<String> clientRoleList = (List<String>) clientRoles.get("roles");
        roles.addAll(clientRoleList);
      }
    }

    return roles;
  }

  @Override
  protected TokenType getTokenType() {
    return TokenType.KEYCLOAK;
  }

  @Override
  protected String extractFirstName(Jwt jwt) {
    return jwt.getClaimAsString("given_name");
  }

  @Override
  protected String extractLastName(Jwt jwt) {
    return jwt.getClaimAsString("family_name");
  }
}
