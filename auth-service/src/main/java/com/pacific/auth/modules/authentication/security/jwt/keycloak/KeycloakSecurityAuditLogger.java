package com.pacific.auth.modules.authentication.security.jwt.keycloak;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Security audit logger for Keycloak authentication events. Logs authentication successes,
 * failures, and logout events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "auth-service.security.authentication.keycloak.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class KeycloakSecurityAuditLogger {

  private final KeycloakTokenValidationService tokenValidationService;
  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /** Log successful authentication events */
  @EventListener
  public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
    Authentication authentication = event.getAuthentication();
    String timestamp = LocalDateTime.now().format(formatter);

    if (authentication.getPrincipal() instanceof Jwt jwt) {
      String username = jwt.getSubject();
      String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "unknown";

      log.info(
          "AUTHENTICATION_SUCCESS - User: {}, Issuer: {}, Timestamp: {}, Source: OAuth2/Keycloak",
          username,
          issuer,
          timestamp);

      // Log additional details
      log.debug(
          "Authentication details - User: {}, Email: {}, Roles: {}",
          username,
          jwt.getClaimAsString("email"),
          extractRoles(jwt));
    } else {
      log.info(
          "AUTHENTICATION_SUCCESS - User: {}, Timestamp: {}, Source: OAuth2/Keycloak",
          authentication.getName(),
          timestamp);
    }
  }

  /** Log authentication failure events */
  @EventListener
  public void handleAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
    String username = event.getAuthentication().getName();
    String timestamp = LocalDateTime.now().format(formatter);
    String reason = event.getException().getMessage();

    log.warn(
        "AUTHENTICATION_FAILURE - User: {}, Reason: {}, Timestamp: {}, Source: OAuth2/Keycloak",
        username,
        reason,
        timestamp);
  }

  /** Log logout events */
  @EventListener
  public void handleLogoutSuccess(LogoutSuccessEvent event) {
    Authentication authentication = event.getAuthentication();
    String timestamp = LocalDateTime.now().format(formatter);

    if (authentication != null) {
      log.info(
          "LOGOUT_SUCCESS - User: {}, Timestamp: {}, Source: OAuth2/Keycloak",
          authentication.getName(),
          timestamp);
    } else {
      log.info("LOGOUT_SUCCESS - User: unknown, Timestamp: {}, Source: OAuth2/Keycloak", timestamp);
    }
  }

  /** Log custom security events */
  public void logSecurityEvent(String eventType, String username, String details) {
    String timestamp = LocalDateTime.now().format(formatter);
    log.info(
        "SECURITY_EVENT - Type: {}, User: {}, Details: {}, Timestamp: {}, Source: OAuth2/Keycloak",
        eventType,
        username,
        details,
        timestamp);
  }

  /** Log token validation events */
  public void logTokenValidation(String token, boolean success, String reason) {
    String timestamp = LocalDateTime.now().format(formatter);

    if (success) {
      try {
        String username = tokenValidationService.extractUsername(token);
        log.debug(
            "TOKEN_VALIDATION_SUCCESS - User: {}, Timestamp: {}, Source: OAuth2/Keycloak",
            username,
            timestamp);
      } catch (Exception e) {
        log.debug(
            "TOKEN_VALIDATION_SUCCESS - User: unknown, Timestamp: {}, Source: OAuth2/Keycloak",
            timestamp);
      }
    } else {
      log.warn(
          "TOKEN_VALIDATION_FAILURE - Reason: {}, Timestamp: {}, Source: OAuth2/Keycloak",
          reason,
          timestamp);
    }
  }

  /** Log role-based access events */
  public void logRoleAccess(String username, String resource, String action, boolean granted) {
    String timestamp = LocalDateTime.now().format(formatter);
    String status = granted ? "GRANTED" : "DENIED";

    log.info(
        "ROLE_ACCESS - User: {}, Resource: {}, Action: {}, Status: {}, Timestamp: {}, Source: OAuth2/Keycloak",
        username,
        resource,
        action,
        status,
        timestamp);
  }

  /** Extract roles from JWT for logging */
  private String extractRoles(Jwt jwt) {
    try {
      Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
      if (realmAccess != null && realmAccess.containsKey("roles")) {
        @SuppressWarnings("unchecked")
        java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");
        return String.join(",", roles);
      }
    } catch (Exception e) {
      log.debug("Failed to extract roles for logging: {}", e.getMessage());
    }
    return "unknown";
  }
}
