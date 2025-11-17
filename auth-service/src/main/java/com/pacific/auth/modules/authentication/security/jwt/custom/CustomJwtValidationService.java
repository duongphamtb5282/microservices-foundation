package com.pacific.auth.modules.authentication.security.jwt.custom;

import com.pacific.auth.modules.authentication.security.jwt.common.AbstractJwtValidationService;
import com.pacific.auth.modules.authentication.security.jwt.common.JwtTokenValidationService;
import com.pacific.auth.modules.authentication.security.jwt.common.JwtValidationResult;
import com.pacific.auth.modules.authentication.security.jwt.common.TokenType;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

/**
 * Service for validating custom JWT tokens and extracting user information. Handles validation of
 * tokens issued by our application.
 */
@Service
@Slf4j
@ConditionalOnProperty(
    name = "auth-service.security.authentication.jwt.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class CustomJwtValidationService extends AbstractJwtValidationService
    implements JwtTokenValidationService {

  public CustomJwtValidationService(@Qualifier("customJwtDecoder") JwtDecoder jwtDecoder) {
    super(jwtDecoder);
  }

  @Override
  public JwtValidationResult validateToken(String token) {
    try {
      log.debug("Validating custom JWT token");
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
    if (issuer == null || !issuer.equals("auth-service")) {
      throw new IllegalArgumentException("Invalid token issuer: " + issuer);
    }

    // Check token type
    String tokenType = jwt.getClaimAsString("type");
    if (tokenType == null || !"access".equals(tokenType)) {
      throw new IllegalArgumentException("Invalid token type: " + tokenType);
    }
  }

  @Override
  protected List<String> extractRoles(Jwt jwt) {
    List<String> roles = jwt.getClaimAsStringList("roles");
    if (roles == null) {
      roles = List.of("USER"); // Default role
    }
    return roles;
  }

  @Override
  protected TokenType getTokenType() {
    return TokenType.CUSTOM;
  }

  @Override
  protected String extractFirstName(Jwt jwt) {
    return jwt.getClaimAsString("firstName");
  }

  @Override
  protected String extractLastName(Jwt jwt) {
    return jwt.getClaimAsString("lastName");
  }
}
