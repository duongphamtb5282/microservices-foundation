package com.pacific.auth.modules.authentication.security.jwt.keycloak;

import com.pacific.auth.modules.authentication.security.jwt.common.AbstractJwtAuthenticationProvider;
import com.pacific.auth.modules.authentication.security.jwt.common.TokenType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Custom authentication provider for Keycloak JWT tokens. Validates JWT tokens issued by Keycloak.
 */
@Component
@Slf4j
@ConditionalOnProperty(
    name = "auth-service.security.authentication.keycloak.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class KeycloakJwtAuthenticationProvider
    extends AbstractJwtAuthenticationProvider<KeycloakJwtAuthenticationToken> {

  private final KeycloakProperties keycloakProperties;

  public KeycloakJwtAuthenticationProvider(
      @Qualifier("keycloakJwtDecoder") JwtDecoder keycloakJwtDecoder,
      KeycloakProperties keycloakProperties) {
    super(keycloakJwtDecoder);
    this.keycloakProperties = keycloakProperties;
  }

  @Override
  public boolean supports(Class<?> authentication) {
    log.debug("Authentication support check - class: {}, supported: {}", authentication.getName());
    return KeycloakJwtAuthenticationToken.class.isAssignableFrom(authentication);
  }

  @Override
  protected void validateJwt(Jwt jwt) {
    // Common JWT validation
    log.debug("Validating JWT issuer1111");
    validateCommonJwt(jwt);

    // Keycloak-specific validation
    String issuer = jwt.getClaimAsString("iss");
    String expectedIssuer = keycloakProperties.getIssuerUri();

    log.info("Validating JWT issuer");
    log.info("Actual issuer: {}", issuer);
    log.info("Expected issuer: {}", expectedIssuer);

    if (issuer == null || !issuer.equals(expectedIssuer)) {
      throw new BadCredentialsException("Invalid JWT issuer: " + issuer);
    }

    // Check audience only if configured to do so
    if (keycloakProperties.isVerifyTokenAudience()) {
      List<String> audience = jwt.getAudience();
      if (audience == null || audience.isEmpty()) {
        throw new BadCredentialsException("JWT token missing audience");
      }

      String clientId = keycloakProperties.getClientId();
      if (!audience.contains(clientId)) {
        throw new BadCredentialsException(
            "JWT token audience does not match client ID: " + clientId);
      }
    } else {
      log.debug("Skipping audience verification (verify-token-audience=false)");
    }
  }

  @Override
  protected Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
    List<GrantedAuthority> authorities = new ArrayList<>();

    // Extract realm roles
    Collection<GrantedAuthority> realmRoles = extractRealmRoles(jwt);
    authorities.addAll(realmRoles);

    // Extract client roles
    Collection<GrantedAuthority> clientRoles =
        extractClientRoles(jwt, keycloakProperties.getResource());
    authorities.addAll(clientRoles);

    // Add default user role if no roles found
    addDefaultUserRole(authorities);

    return authorities;
  }

  @Override
  protected KeycloakJwtAuthenticationToken createAuthenticatedToken(
      String jwtToken, String username, Collection<GrantedAuthority> authorities) {
    return new KeycloakJwtAuthenticationToken(jwtToken, username, authorities);
  }

  @Override
  protected TokenType getTokenType() {
    return TokenType.KEYCLOAK;
  }
}
