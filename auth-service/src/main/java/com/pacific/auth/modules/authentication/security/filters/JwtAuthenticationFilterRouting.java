package com.pacific.auth.modules.authentication.security.filters;

import com.pacific.auth.modules.authentication.security.jwt.keycloak.KeycloakJwtAuthenticationProvider;
import com.pacific.auth.modules.authentication.security.jwt.keycloak.KeycloakJwtAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT authentication filter backed by the Keycloak authentication provider. Token-type routing was
 * removed with the dual-mode stack (S-06): every Bearer token is validated against Keycloak.
 *
 * <p>The provider chain is fully self-contained: authenticateToken catches every attempt, so a
 * failed token never aborts the request — downstream security filters decide.
 */
@Slf4j
public class JwtAuthenticationFilterRouting extends OncePerRequestFilter {

  private final KeycloakJwtAuthenticationProvider keycloakJwtProvider;
  private AuthenticationManager authenticationManager;

  public JwtAuthenticationFilterRouting(KeycloakJwtAuthenticationProvider keycloakJwtProvider) {
    this.keycloakJwtProvider = keycloakJwtProvider;
  }

  public void setAuthenticationManager(AuthenticationManager authenticationManager) {
    this.authenticationManager = authenticationManager;
  }

  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    log.info(
        "🔍 JWT Authentication Filter called for: {} {}",
        request.getMethod(),
        request.getRequestURI());

    String token = extractToken(request);
    if (token == null) {
      log.info("❌ No JWT token found in request, continuing filter chain");
      filterChain.doFilter(request, response);
      return;
    }

    log.info("✅ JWT token found: {}...", token.substring(0, Math.min(20, token.length())));

    // The provider chain is fully self-contained: authenticateToken catches every attempt, so a
    // failed token never aborts the request — downstream security filters decide.
    Authentication authentication = authenticateToken(token);
    if (authentication != null && authentication.isAuthenticated()) {
      SecurityContextHolder.getContext().setAuthentication(authentication);
      log.debug(
          "Authentication successful for token type: {}",
          authentication.getClass().getSimpleName());
    } else {
      log.debug("Authentication failed for token — continuing unauthenticated");
    }

    filterChain.doFilter(request, response);
  }

  /**
   * Run the ordered authentication chain exactly once and return the first success: authentication
   * manager (when configured) → Keycloak JWT provider. Every attempt is individually caught so a
   * failure in one provider falls through to the next instead of failing the request.
   */
  private Authentication authenticateToken(String token) {
    KeycloakJwtAuthenticationToken authentication = new KeycloakJwtAuthenticationToken(token);

    if (authenticationManager != null) {
      Authentication auth =
          tryAttempt(
              () -> authenticationManager.authenticate(authentication),
              "authentication manager");
      if (auth != null) {
        return auth;
      }
    }

    Authentication auth =
        tryAttempt(() -> keycloakJwtProvider.authenticate(authentication), "Keycloak JWT provider");
    if (auth != null) {
      return auth;
    }

    log.debug("All authentication methods failed");
    return null;
  }

  /** Run one authentication attempt; a failure falls through to the next provider. */
  private Authentication tryAttempt(Supplier<Authentication> attempt, String providerName) {
    try {
      Authentication auth = attempt.get();
      if (auth != null && auth.isAuthenticated()) {
        log.debug("Authentication successful via {}", providerName);
        return auth;
      }
      log.debug("{} returned unauthenticated result", providerName);
      return null;
    } catch (Exception e) {
      // Deliberate fallback chain: a rejected or failed token in one provider must not fail the
      // request — log and fall through to the next provider (single catch: AuthenticationException
      // is a subclass of RuntimeException and needs no special handling here).
      log.warn("{} rejected token (trying next provider): {}", providerName, e.getMessage());
      return null;
    }
  }

  /** Extract JWT token from request. Supports both Authorization header and custom header. */
  private String extractToken(HttpServletRequest request) {
    // Try Authorization header first
    String authHeader = request.getHeader("Authorization");
    if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }

    // Try custom header as fallback
    String customHeader = request.getHeader("X-Auth-Token");
    if (StringUtils.hasText(customHeader)) {
      return customHeader;
    }

    return null;
  }
}
