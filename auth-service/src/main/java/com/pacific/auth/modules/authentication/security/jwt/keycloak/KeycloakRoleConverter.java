package com.pacific.auth.modules.authentication.security.jwt.keycloak;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Converts JWT claims to Spring Security authorities for Keycloak tokens. Handles both realm roles
 * and client-specific roles.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "auth-service.security.authentication.keycloak.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  private final KeycloakProperties keycloakProperties;

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    log.debug("Converting JWT claims to authorities for subject: {}", jwt.getSubject());

    List<GrantedAuthority> authorities = new ArrayList<>();

    // Extract realm roles
    Collection<GrantedAuthority> realmRoles = extractRealmRoles(jwt);
    authorities.addAll(realmRoles);

    // Extract client roles
    Collection<GrantedAuthority> clientRoles = extractClientRoles(jwt);
    authorities.addAll(clientRoles);

    // Add default user role if no roles found
    if (authorities.isEmpty()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    }

    log.debug("Extracted {} authorities for subject: {}", authorities.size(), jwt.getSubject());
    return authorities;
  }

  /** Extract realm roles from JWT claims */
  private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
    if (realmAccess != null && realmAccess.containsKey("roles")) {
      @SuppressWarnings("unchecked")
      List<String> realmRoles = (List<String>) realmAccess.get("roles");
      return realmRoles.stream()
          .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
          .collect(Collectors.toList());
    }
    return new ArrayList<>();
  }

  /** Extract client-specific roles from JWT claims */
  private Collection<GrantedAuthority> extractClientRoles(Jwt jwt) {
    Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
    if (resourceAccess != null && keycloakProperties.getResource() != null) {
      @SuppressWarnings("unchecked")
      Map<String, Object> clientRoles =
          (Map<String, Object>) resourceAccess.get(keycloakProperties.getResource());
      if (clientRoles != null && clientRoles.containsKey("roles")) {
        @SuppressWarnings("unchecked")
        List<String> clientRoleList = (List<String>) clientRoles.get("roles");
        return clientRoleList.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            .collect(Collectors.toList());
      }
    }
    return new ArrayList<>();
  }
}
