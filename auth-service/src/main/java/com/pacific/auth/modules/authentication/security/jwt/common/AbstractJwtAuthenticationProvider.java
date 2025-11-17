package com.pacific.auth.modules.authentication.security.jwt.common;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Abstract base class for JWT authentication providers. Provides common functionality for JWT token
 * validation and authority extraction.
 */
@Slf4j
public abstract class AbstractJwtAuthenticationProvider<T extends JwtAuthenticationToken>
    implements AuthenticationProvider {

  protected final JwtDecoder jwtDecoder;

  protected AbstractJwtAuthenticationProvider(JwtDecoder jwtDecoder) {
    this.jwtDecoder = jwtDecoder;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    log.debug("Starting authentication process with provider: {}", this.getClass().getSimpleName());

    if (!supports(authentication.getClass())) {
      log.debug("Authentication type not supported: {}", authentication.getClass());
      return null;
    }

    @SuppressWarnings("unchecked")
    T authToken = (T) authentication;
    String jwtToken = authToken.getJwtToken();
    log.info("Authenticating {} arerree", jwtToken);
    try {
      log.info("Authenticating {} token", getTokenType().getDisplayName());

      // Decode and validate the JWT token
      Jwt jwt = jwtDecoder.decode(jwtToken);

      // Perform token-specific validation
      validateJwt(jwt);

      // Extract user information
      String username = extractUsername(jwt);
      log.info("usernameeee {} token", username);
      if (username == null || username.isEmpty()) {
        throw new BadCredentialsException("JWT token missing subject");
      }

      // Extract authorities from JWT claims
      Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

      log.debug(
          "{} authentication successful for user: {}", getTokenType().getDisplayName(), username);

      // Create authenticated token
      return createAuthenticatedToken(jwtToken, username, authorities);

    } catch (JwtException e) {
      log.warn("{} authentication failed: {}", getTokenType().getDisplayName(), e.getMessage());
      throw new BadCredentialsException("Invalid JWT token", e);
    } catch (Exception e) {
      log.error("Unexpected error during {} authentication", getTokenType().getDisplayName(), e);
      throw new BadCredentialsException("Authentication failed", e);
    }
  }

  /** Extract username from JWT token */
  protected String extractUsername(Jwt jwt) {
    return jwt.getSubject();
  }

  /** Validate JWT token - to be implemented by subclasses */
  protected abstract void validateJwt(Jwt jwt);

  /** Extract authorities from JWT claims - to be implemented by subclasses */
  protected abstract Collection<GrantedAuthority> extractAuthorities(Jwt jwt);

  /** Create authenticated token - to be implemented by subclasses */
  protected abstract T createAuthenticatedToken(
      String jwtToken, String username, Collection<GrantedAuthority> authorities);

  /** Get token type for logging - to be implemented by subclasses */
  protected abstract TokenType getTokenType();

  /** Common JWT validation */
  protected void validateCommonJwt(Jwt jwt) {
    // Check expiration
    if (jwt.getExpiresAt() != null && jwt.getExpiresAt().isBefore(Instant.now())) {
      throw new BadCredentialsException("JWT token has expired");
    }
  }

  /** Extract realm roles from JWT claims */
  protected Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
    List<GrantedAuthority> authorities = new ArrayList<>();

    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
    if (realmAccess != null) {
      @SuppressWarnings("unchecked")
      List<String> realmRoles = (List<String>) realmAccess.get("roles");
      if (realmRoles != null) {
        for (String role : realmRoles) {
          String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
          authorities.add(new SimpleGrantedAuthority(authority));
        }
      }
    }

    return authorities;
  }

  /** Extract client roles from JWT claims */
  protected Collection<GrantedAuthority> extractClientRoles(Jwt jwt, String clientId) {
    List<GrantedAuthority> authorities = new ArrayList<>();

    Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
    if (resourceAccess != null && clientId != null) {
      @SuppressWarnings("unchecked")
      Map<String, Object> clientRoles = (Map<String, Object>) resourceAccess.get(clientId);
      if (clientRoles != null) {
        @SuppressWarnings("unchecked")
        List<String> clientRoleList = (List<String>) clientRoles.get("roles");
        if (clientRoleList != null) {
          for (String role : clientRoleList) {
            String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
            authorities.add(new SimpleGrantedAuthority(authority));
          }
        }
      }
    }

    return authorities;
  }

  /** Extract custom roles from JWT claims */
  protected Collection<GrantedAuthority> extractCustomRoles(Jwt jwt, String claimName) {
    List<GrantedAuthority> authorities = new ArrayList<>();

    List<String> roles = jwt.getClaimAsStringList(claimName);
    if (roles != null) {
      for (String role : roles) {
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
        authorities.add(new SimpleGrantedAuthority(authority));
      }
    }

    return authorities;
  }

  /** Add default user role if no roles found */
  protected void addDefaultUserRole(Collection<GrantedAuthority> authorities) {
    if (authorities.isEmpty()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    }
  }
}
