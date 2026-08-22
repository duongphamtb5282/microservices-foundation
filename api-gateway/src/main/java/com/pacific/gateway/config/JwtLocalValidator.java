package com.pacific.gateway.config;

import com.pacific.gateway.exception.JwtValidationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import java.security.Key;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Verifies Bearer tokens locally with jjwt: RS256 signature against the Keycloak JWKS keys (looked
 * up by {@code kid}), issuer equality with {@code GATEWAY_ISSUER}, and expiry. No round-trip to the
 * Auth Service is performed.
 */
@Service
@Slf4j
public class JwtLocalValidator {

  private final KeycloakJwksService jwksService;

  @Value("${GATEWAY_ISSUER:http://localhost:8080/realms/master}")
  private String expectedIssuer;

  public JwtLocalValidator(KeycloakJwksService jwksService) {
    this.jwksService = jwksService;
  }

  /**
   * Validates the given raw Bearer token (without the "Bearer " prefix) locally.
   *
   * @param token the raw JWT string
   * @return the extracted subject, username and realm roles
   * @throws JwtValidationException if the token is invalid, expired, or no signing key is available
   */
  public TokenValidationResult validateToken(String token) {
    try {
      Claims claims =
          Jwts.parserBuilder()
              .setSigningKeyResolver(
                  new SigningKeyResolverAdapter() {
                    @Override
                    public Key resolveSigningKey(JwsHeader header, Claims claims) {
                      RSAPublicKey key = jwksService.getKey(header.getKeyId());
                      if (key == null) {
                        log.debug("No cached JWKS key for kid: {}", header.getKeyId());
                      }
                      return key;
                    }
                  })
              .requireIssuer(expectedIssuer)
              .build()
              .parseClaimsJws(token)
              .getBody();

      String subject = claims.getSubject();
      String username = claims.get("preferred_username", String.class);
      if (username == null || username.isBlank()) {
        username = subject;
      }
      List<String> roles = extractRoles(claims);
      log.debug("Token verified locally for user: {}", username);
      return new TokenValidationResult(true, subject, username, roles);
    } catch (Exception e) {
      log.debug("Local JWT validation failed: {}", e.getMessage());
      throw new JwtValidationException("Invalid or expired token", e);
    }
  }

  /** Extracts the {@code realm_access.roles} claim as a list of realm role names. */
  @SuppressWarnings("unchecked")
  private List<String> extractRoles(Claims claims) {
    Object realmAccessObject = claims.get("realm_access");
    if (realmAccessObject instanceof Map) {
      Map<String, Object> realmAccess = (Map<String, Object>) realmAccessObject;
      Object rolesObject = realmAccess.get("roles");
      if (rolesObject instanceof List) {
        List<String> roles = (List<String>) rolesObject;
        return List.copyOf(roles);
      }
    }
    return List.of();
  }

  /** Result of a successful local JWT validation. */
  public record TokenValidationResult(
      boolean valid, String userId, String username, List<String> roles) {}
}
