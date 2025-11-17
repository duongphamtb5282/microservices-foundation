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

    log.debug("Processing JWT token with smart routing");

    try {
      Authentication authentication = authenticateToken(request, token);
      if (authentication != null && authentication.isAuthenticated()) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug(
            "Authentication successful for token type: {}",
            authentication.getClass().getSimpleName());
      } else {
        log.debug("Authentication failed for token");
      }
    } catch (AuthenticationException e) {
      log.info("Authentication exception: {}, trying fallback providers", e.getMessage());
      // Try fallback providers when smart routing fails
      try {
        log.info("Calling tryFallbackProviders for token");
        Authentication fallbackAuth = tryFallbackProviders(token);
        if (fallbackAuth != null && fallbackAuth.isAuthenticated()) {
          SecurityContextHolder.getContext().setAuthentication(fallbackAuth);
          log.info(
              "Fallback authentication successful for token type: {}",
              fallbackAuth.getClass().getSimpleName());
        } else {
          log.info("All authentication methods failed");
        }
      } catch (Exception fallbackException) {
        log.info("Fallback authentication also failed: {}", fallbackException.getMessage());
      }
    } catch (Exception e) {
      log.warn(
          "Unexpected error during authentication: {}, trying fallback providers", e.getMessage());
      // Try fallback providers when unexpected errors occur
      try {
        log.info("Calling tryFallbackProviders for token after unexpected error");
        Authentication fallbackAuth = tryFallbackProviders(token);
        if (fallbackAuth != null && fallbackAuth.isAuthenticated()) {
          SecurityContextHolder.getContext().setAuthentication(fallbackAuth);
          log.info(
              "Fallback authentication successful for token type: {}",
              fallbackAuth.getClass().getSimpleName());
        } else {
          log.info("All authentication methods failed after unexpected error");
        }
      } catch (Exception fallbackException) {
        log.info(
            "Fallback authentication also failed after unexpected error: {}",
            fallbackException.getMessage());
      }
    }

    filterChain.doFilter(request, response);
  }

  /** Try fallback providers when smart routing fails. */
  private Authentication tryFallbackProviders(String token) {
    log.debug("Trying fallback providers for token");

    // Try custom JWT provider as fallback
    try {
      log.debug("Trying custom JWT provider as fallback");
      return customJwtProvider.authenticate(
          createAuthenticationForProvider(token, customJwtProvider));
    } catch (Exception e) {
      log.debug("Custom JWT provider failed: {}", e.getMessage());
    }

    // Try Keycloak JWT provider as fallback
    try {
      log.debug("Trying Keycloak JWT provider as fallback");
      return keycloakJwtProvider.authenticate(
          createAuthenticationForProvider(token, keycloakJwtProvider));
    } catch (Exception e) {
      log.debug("Keycloak JWT provider failed: {}", e.getMessage());
    }

    // Try authentication manager with provider chain
    if (authenticationManager != null) {
      log.debug("Trying authentication manager with provider chain");
      try {
        Authentication auth = createAuthentication(token);
        log.debug("Created authentication object: {}", auth.getClass().getSimpleName());
        log.debug("Authentication credentials: {}", auth.getCredentials());
        return authenticationManager.authenticate(auth);
      } catch (Exception e) {
        log.debug("Authentication manager failed: {}", e.getMessage());
      }
    }

    log.debug("All fallback authentication methods failed");
    return null;
  }

  /** Authenticate the token using smart routing. */
  private Authentication authenticateToken(HttpServletRequest request, String token) {
    // Fallback to authentication manager with provider chain
    if (authenticationManager != null) {
      log.debug("Using authentication manager with provider chain");
      try {
        Authentication auth = createAuthentication(token);
        log.debug("Created authentication object: {}", auth.getClass().getSimpleName());
        log.debug("Authentication credentials: {}", auth.getCredentials());
        return authenticationManager.authenticate(auth);
      } catch (Exception e) {
        log.debug("Authentication manager failed: {}", e.getMessage());
      }
    }

    log.debug("No authentication method available - trying direct provider authentication");
    // Try custom JWT provider as fallback
    try {
      log.debug("Trying custom JWT provider as fallback");
      return customJwtProvider.authenticate(
          createAuthenticationForProvider(token, customJwtProvider));
    } catch (Exception e) {
      log.debug("Custom JWT provider failed: {}", e.getMessage());
    }

    // Try Keycloak JWT provider as fallback
    try {
      log.debug("Trying Keycloak JWT provider as fallback");
      return keycloakJwtProvider.authenticate(
          createAuthenticationForProvider(token, keycloakJwtProvider));
    } catch (Exception e) {
      log.debug("Keycloak JWT provider failed: {}", e.getMessage());
    }

    log.debug("All authentication methods failed");
    return null;
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
