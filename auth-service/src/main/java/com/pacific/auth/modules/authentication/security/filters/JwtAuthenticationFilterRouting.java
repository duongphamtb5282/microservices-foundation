package com.pacific.auth.modules.authentication.security.filters;

import com.pacific.auth.modules.authentication.security.jwt.custom.CustomJwtAuthenticationProvider;
import com.pacific.auth.modules.authentication.security.jwt.custom.CustomJwtAuthenticationToken;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enhanced JWT authentication filter that uses smart token routing to determine the appropriate
 * authentication provider for each token.
 *
 * <p>This filter uses header-based routing via X-Token-Type header: - "custom": Routes to custom
 * JWT authentication provider - "keycloak": Routes to Keycloak JWT authentication provider - No
 * header or unknown value: Falls back to authentication manager
 */
@Slf4j
public class JwtAuthenticationFilterRouting extends OncePerRequestFilter {

  private final CustomJwtAuthenticationProvider customJwtProvider;
  private final KeycloakJwtAuthenticationProvider keycloakJwtProvider;
  private AuthenticationManager authenticationManager;

  public JwtAuthenticationFilterRouting(
      CustomJwtAuthenticationProvider customJwtProvider,
      KeycloakJwtAuthenticationProvider keycloakJwtProvider) {
    this.customJwtProvider = customJwtProvider;
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

    try {
      // The provider chain is fully self-contained: every attempt is caught inside
      // authenticateToken, so this outer catch only guards genuinely unexpected failures.
      // A failed token never aborts the request — downstream security filters decide.
      Authentication authentication = authenticateToken(token);
      if (authentication != null && authentication.isAuthenticated()) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug(
            "Authentication successful for token type: {}",
            authentication.getClass().getSimpleName());
      } else {
        log.debug("Authentication failed for token — continuing unauthenticated");
      }
    } catch (Exception e) {
      log.warn("Unexpected error during JWT authentication: {}", e.getMessage(), e);
    }

    filterChain.doFilter(request, response);
  }

  /**
   * Run the ordered authentication chain exactly once and return the first success: authentication
   * manager (when configured) → custom JWT provider → Keycloak JWT provider. Every attempt is
   * individually caught so a failure in one provider falls through to the next instead of failing
   * the request.
   */
  private Authentication authenticateToken(String token) {
    if (authenticationManager != null) {
      Authentication auth =
          tryAttempt(
              () -> authenticationManager.authenticate(createAuthentication(token)),
              "authentication manager");
      if (auth != null) {
        return auth;
      }
    }

    Authentication auth =
        tryAttempt(
            () ->
                customJwtProvider.authenticate(
                    createAuthenticationForProvider(token, customJwtProvider)),
            "custom JWT provider");
    if (auth != null) {
      return auth;
    }

    auth =
        tryAttempt(
            () ->
                keycloakJwtProvider.authenticate(
                    createAuthenticationForProvider(token, keycloakJwtProvider)),
            "Keycloak JWT provider");
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
    } catch (AuthenticationException e) {
      // Deliberate fallback chain: an invalid token in one provider must not fail the request.
      log.warn("{} rejected token (trying next provider): {}", providerName, e.getMessage());
      return null;
    } catch (Exception e) {
      // Deliberate fallback chain: a provider failure must not fail the request.
      log.warn("{} failed (trying next provider): {}", providerName, e.getMessage());
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

  /**
   * Create authentication object from token. Creates the appropriate authentication token based on
   * the provider.
   */
  private Authentication createAuthentication(String token) {
    // For custom JWT provider, create CustomJwtAuthenticationToken
    return new CustomJwtAuthenticationToken(token);
  }

  /** Create authentication object for specific provider. */
  private Authentication createAuthenticationForProvider(String token, Object provider) {
    if (provider instanceof CustomJwtAuthenticationProvider) {
      return new CustomJwtAuthenticationToken(token);
    } else if (provider instanceof KeycloakJwtAuthenticationProvider) {
      return new KeycloakJwtAuthenticationToken(token);
    } else {
      // Fallback to generic token
      return new UsernamePasswordAuthenticationToken(token, null);
    }
  }
}
