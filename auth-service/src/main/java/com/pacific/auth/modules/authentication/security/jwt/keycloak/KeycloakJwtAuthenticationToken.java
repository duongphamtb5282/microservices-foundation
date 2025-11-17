package com.pacific.auth.modules.authentication.security.jwt.keycloak;

import com.pacific.auth.modules.authentication.security.jwt.common.JwtAuthenticationToken;
import com.pacific.auth.modules.authentication.security.jwt.common.TokenType;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Custom authentication token for Keycloak JWT authentication. This token represents an
 * authentication request with a JWT token from Keycloak.
 */
public class KeycloakJwtAuthenticationToken extends AbstractAuthenticationToken
    implements JwtAuthenticationToken {

  private final String jwtToken;
  private Object principal;
  private Object credentials;

  /** Constructor for unauthenticated token (before authentication) */
  public KeycloakJwtAuthenticationToken(String jwtToken) {
    super(null);
    this.jwtToken = jwtToken;
    this.principal = null;
    this.credentials = jwtToken;
    setAuthenticated(false);
  }

  /** Constructor for authenticated token (after successful authentication) */
  public KeycloakJwtAuthenticationToken(
      String jwtToken, Object principal, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.jwtToken = jwtToken;
    this.principal = principal;
    this.credentials = jwtToken;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return credentials;
  }

  @Override
  public Object getPrincipal() {
    return principal;
  }

  @Override
  public String getJwtToken() {
    return jwtToken;
  }

  @Override
  public TokenType getTokenType() {
    return TokenType.KEYCLOAK;
  }
}
