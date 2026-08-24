package com.pacific.auth.modules.authentication.security.filters;

import com.pacific.auth.modules.authentication.security.jwt.keycloak.KeycloakJwtAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that turns a Bearer token into a {@link KeycloakJwtAuthenticationToken} and hands it to
 * the authentication manager. Dual-mode token routing was removed (S-06): Keycloak is the only
 * token issuer, so every token is validated against Keycloak's JWKS; an unknown token simply fails
 * validation and the request continues unauthenticated.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private AuthenticationManager jwtAuthenticationManager;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);

      try {
        Authentication authentication = new KeycloakJwtAuthenticationToken(token);

        if (jwtAuthenticationManager != null) {
          // Authenticate against the Keycloak JWT provider
          Authentication authResult = jwtAuthenticationManager.authenticate(authentication);

          if (authResult != null && authResult.isAuthenticated()) {
            SecurityContextHolder.getContext().setAuthentication(authResult);
            log.debug(
                "Authentication successful for token type: {}",
                authResult.getClass().getSimpleName());
          }
        }

      } catch (Exception e) {
        // Deliberate fallback: continue without setting authentication so downstream
        // filters/providers can attempt their own token validation.
        log.warn(
            "JwtAuthenticationFilter authentication failed; continuing as fallback: {}",
            e.getMessage());
      }
    }

    filterChain.doFilter(request, response);
  }

  /** Set the authentication manager (called by ProvidersSecurityConfiguration) */
  public void setAuthenticationManager(AuthenticationManager authenticationManager) {
    this.jwtAuthenticationManager = authenticationManager;
  }
}
