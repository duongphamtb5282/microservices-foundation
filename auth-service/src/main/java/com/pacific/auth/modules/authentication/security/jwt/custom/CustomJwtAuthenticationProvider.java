package com.pacific.auth.modules.authentication.security.jwt.custom;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Custom authentication provider for custom JWT tokens. Validates JWT tokens issued by our
 * application.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "auth-service.security.authentication.mode", havingValue = "custom")
public class CustomJwtAuthenticationProvider
    extends AbstractJwtAuthenticationProvider<CustomJwtAuthenticationToken> {

  public CustomJwtAuthenticationProvider(
      @Qualifier("customJwtDecoder") JwtDecoder customJwtDecoder) {
    super(customJwtDecoder);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return CustomJwtAuthenticationToken.class.isAssignableFrom(authentication);
  }

  @Override
  protected void validateJwt(Jwt jwt) {
    // Common JWT validation
    validateCommonJwt(jwt);

    // Custom-specific validation
    String issuer = jwt.getClaimAsString("iss");
    // Accept "auth-service" and environment-specific issuers like "auth-service-dev",
    // "auth-service-prod", etc.
    if (issuer == null || !issuer.startsWith("auth-service")) {
      throw new BadCredentialsException("Invalid JWT issuer: " + issuer);
    }

    // Check token type
    String tokenType = jwt.getClaimAsString("type");
    if (tokenType == null || !"access".equals(tokenType)) {
      throw new BadCredentialsException("Invalid JWT token type: " + tokenType);
    }
  }

  @Override
  protected Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
    List<GrantedAuthority> authorities = new ArrayList<>();

    // Add default user role
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

    // Extract custom roles from JWT claims if any
    Collection<GrantedAuthority> customRoles = extractCustomRoles(jwt, "roles");
    authorities.addAll(customRoles);

    return authorities;
  }

  @Override
  protected CustomJwtAuthenticationToken createAuthenticatedToken(
      String jwtToken, String username, Collection<GrantedAuthority> authorities) {
    return new CustomJwtAuthenticationToken(jwtToken, username, authorities);
  }

  @Override
  protected TokenType getTokenType() {
    return TokenType.CUSTOM;
  }
}
