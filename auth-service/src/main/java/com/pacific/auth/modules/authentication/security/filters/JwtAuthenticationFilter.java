package com.pacific.auth.modules.authentication.security.filters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pacific.auth.modules.authentication.security.jwt.custom.CustomJwtAuthenticationToken;
import com.pacific.auth.modules.authentication.security.jwt.keycloak.KeycloakJwtAuthenticationToken;
import com.pacific.auth.modules.authentication.security.jwt.keycloak.KeycloakProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Custom filter that detects JWT token type and creates appropriate authentication token. Routes
 * between custom JWT and Keycloak JWT based on token issuer.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${auth-service.security.authentication.jwt.issuer}")
  private String customJwtIssuer;

  @Autowired(required = false)
  private KeycloakProperties keycloakProperties;

  @Autowired(required = false)
  private com.pacific.auth.config.security.JwtValidationProperties jwtValidationProperties;

  private AuthenticationManager jwtAuthenticationManager;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);

      try {
        // Detect token type and create appropriate authentication token
        Authentication authentication = createAuthenticationToken(token);

        if (authentication != null && jwtAuthenticationManager != null) {
          // Authenticate using the appropriate provider
          Authentication authResult = jwtAuthenticationManager.authenticate(authentication);

          if (authResult != null && authResult.isAuthenticated()) {
            SecurityContextHolder.getContext().setAuthentication(authResult);
            log.debug(
                "Authentication successful for token type: {}",
                authentication.getClass().getSimpleName());
          }
        }

      } catch (Exception e) {
        log.debug("Authentication failed: {}", e.getMessage());
        // Continue without setting authentication (will be handled by other filters)
      }
    }

    filterChain.doFilter(request, response);
  }

  /** Set the authentication manager (called by JwtSecurityConfig) */
  public void setAuthenticationManager(AuthenticationManager authenticationManager) {
    this.jwtAuthenticationManager = authenticationManager;
  }

  /** Create appropriate authentication token based on JWT issuer */
  private Authentication createAuthenticationToken(String token) {
    try {
      String issuer = extractIssuerFromToken(token);

      if (isKeycloakToken(issuer)) {
        log.debug("Detected Keycloak token, creating KeycloakJwtAuthenticationToken");
        return new KeycloakJwtAuthenticationToken(token);
      } else if (isCustomToken(issuer)) {
        log.debug("Detected custom token, creating CustomJwtAuthenticationToken");
        return new CustomJwtAuthenticationToken(token);
      } else {
        log.debug("Unknown token issuer: {}, trying custom token as fallback", issuer);
        return new CustomJwtAuthenticationToken(token);
      }

    } catch (Exception e) {
      log.debug("Failed to detect token type, using custom token as fallback: {}", e.getMessage());
      return new CustomJwtAuthenticationToken(token);
    }
  }

  /** Extract issuer from JWT token without full validation */
  private String extractIssuerFromToken(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3) {
        log.debug("Invalid JWT token format: expected 3 parts, got {}", parts.length);
        return null;
      }

      // Decode payload (base64url)
      String payload = parts[1];
      // Add padding if needed
      while (payload.length() % 4 != 0) {
        payload += "=";
      }

      byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
      String payloadJson = new String(decodedBytes);

      // Parse JSON to get issuer
      JsonNode jsonNode = objectMapper.readTree(payloadJson);
      JsonNode issuerNode = jsonNode.get("iss");

      if (issuerNode == null) {
        log.debug("JWT token does not contain 'iss' (issuer) claim");
        return null;
      }

      return issuerNode.asText();

    } catch (Exception e) {
      log.debug("Failed to extract issuer from token: {}", e.getMessage());
      return null;
    }
  }

  /** Check if token is from Keycloak using configurable patterns */
  private boolean isKeycloakToken(String issuer) {
    if (issuer == null) {
      return false;
    }

    // Check if issuer contains "keycloak" or the configured realm
    if (issuer.contains("keycloak")) {
      return true;
    }

    // Check against configured Keycloak properties if available
    if (keycloakProperties != null) {
      String keycloakServerUrl = keycloakProperties.getServerUrl();
      String keycloakRealm = keycloakProperties.getRealm();

      return (keycloakServerUrl != null && issuer.contains(keycloakServerUrl))
          || (keycloakRealm != null && issuer.contains(keycloakRealm));
    }

    return false;
  }

  /** Check if token is from our custom app using secure issuer validation */
  private boolean isCustomToken(String issuer) {
    if (issuer == null) {
      return false;
    }

    // Use JwtValidationProperties for secure validation if available
    if (jwtValidationProperties != null) {
      return jwtValidationProperties.isAllowedIssuer(issuer);
    }

    // Fallback: Check exact match first
    if (customJwtIssuer != null && customJwtIssuer.equals(issuer)) {
      return true;
    }

    // Final fallback (less secure): Check if issuer contains custom app identifier
    log.warn("JWT validation properties not configured, using fallback issuer validation");
    return issuer.contains("auth-service");
  }
}
