package com.pacific.auth.modules.authentication.security.jwt.keycloak;

import java.util.Collection;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

/**
 * Custom JWT authentication converter for Keycloak tokens. Converts JWT tokens to Spring Security
 * authentication tokens with proper authorities.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "auth-service.security.authentication.keycloak.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class KeycloakJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  private final KeycloakRoleConverter roleConverter;
  private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter =
      new JwtGrantedAuthoritiesConverter();

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    log.debug("Converting JWT to authentication token for subject: {}", jwt.getSubject());

    // Extract authorities from JWT claims
    Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

    // Create JWT authentication token
    JwtAuthenticationToken authToken = new JwtAuthenticationToken(jwt, authorities);

    log.debug(
        "Successfully converted JWT to authentication token with {} authorities",
        authorities.size());

    return authToken;
  }

  /** Extract authorities from JWT claims using both default and custom converters */
  private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
    // Get default authorities from JWT
    Collection<GrantedAuthority> defaultAuthorities = jwtGrantedAuthoritiesConverter.convert(jwt);

    // Get custom authorities from role converter
    Collection<GrantedAuthority> customAuthorities = roleConverter.convert(jwt);

    // Combine both collections
    return Stream.concat(defaultAuthorities.stream(), customAuthorities.stream())
        .distinct()
        .toList();
  }
}
