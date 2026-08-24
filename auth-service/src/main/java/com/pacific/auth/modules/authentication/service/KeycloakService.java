package com.pacific.auth.modules.authentication.service;

import com.pacific.auth.modules.authentication.client.KeycloakAdminClient;
import com.pacific.auth.modules.authentication.client.KeycloakTokenClient;
import com.pacific.auth.modules.authentication.client.dto.KeycloakRoleRepresentation;
import com.pacific.auth.modules.authentication.client.dto.KeycloakTokenResponse;
import com.pacific.auth.modules.authentication.client.dto.KeycloakUserRepresentation;
import com.pacific.core.messaging.circuitbreaker.CircuitBreakerService;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * High-level service for Keycloak operations Wraps Feign clients with business logic and caching
 */
@Service
@RequiredArgsConstructor
@Slf4j
// S-06: same canonical key family as KeycloakProperties and the JWT validation stack
@ConditionalOnProperty(
    name = "auth-service.security.authentication.keycloak.enabled",
    havingValue = "true")
public class KeycloakService {

  private final KeycloakTokenClient tokenClient;
  private final KeycloakAdminClient adminClient;
  private final CircuitBreakerService circuitBreakerService;

  @Value("${auth-service.security.authentication.keycloak.realm:master}")
  private String realm;

  @Value("${auth-service.security.authentication.keycloak.client-id:auth-service}")
  private String clientId;

  @Value("${auth-service.security.authentication.keycloak.credentials-secret:}")
  private String clientSecret;

  // Admin token cache (in production, use proper caching)
  private String cachedAdminToken;
  private long tokenExpiryTime = 0;

  // ============================================================================
  // Token Operations
  // ============================================================================

  /** Login with username and password */
  public KeycloakTokenResponse login(String username, String password) {
    log.info("Logging in user: {}", username);
    return keycloakCall(
        "login",
        () -> tokenClient.getToken(realm, clientId, clientSecret, username, password, "password"));
  }

  /** Refresh access token */
  public KeycloakTokenResponse refreshToken(String refreshToken) {
    log.info("Refreshing token");
    return keycloakCall(
        "refreshToken",
        () ->
            tokenClient.refreshToken(
                realm, clientId, clientSecret, refreshToken, "refresh_token"));
  }

  /** Logout user */
  public void logout(String refreshToken) {
    log.info("Logging out user");
    keycloakCall(
        "logout",
        () -> {
          tokenClient.logout(realm, clientId, clientSecret, refreshToken);
          return null;
        });
  }

  /** Revoke token */
  public void revokeToken(String token) {
    log.info("Revoking token");
    keycloakCall(
        "revokeToken",
        () -> {
          tokenClient.revokeToken(realm, clientId, clientSecret, token);
          return null;
        });
  }

  // ============================================================================
  // User Management
  // ============================================================================

  /** Get all users */
  public List<KeycloakUserRepresentation> getUsers() {
    return keycloakCall("getUsers", () -> adminClient.getUsers(realm, "Bearer " + getAdminToken()));
  }

  /** Get user by ID */
  public KeycloakUserRepresentation getUser(String userId) {
    return keycloakCall(
        "getUser", () -> adminClient.getUser(realm, userId, "Bearer " + getAdminToken()));
  }

  /** Search users by username */
  public List<KeycloakUserRepresentation> searchUsers(String username) {
    return keycloakCall(
        "searchUsers",
        () -> adminClient.searchUsers(realm, username, "Bearer " + getAdminToken()));
  }

  /** Create user */
  public void createUser(KeycloakUserRepresentation user) {
    keycloakCall(
        "createUser",
        () -> {
          adminClient.createUser(realm, user, "Bearer " + getAdminToken());
          return null;
        });
    log.info("Created user: {}", user.getUsername());
  }

  /** Update user */
  public void updateUser(String userId, KeycloakUserRepresentation user) {
    keycloakCall(
        "updateUser",
        () -> {
          adminClient.updateUser(realm, userId, user, "Bearer " + getAdminToken());
          return null;
        });
    log.info("Updated user: {}", userId);
  }

  /** Delete user */
  public void deleteUser(String userId) {
    keycloakCall(
        "deleteUser",
        () -> {
          adminClient.deleteUser(realm, userId, "Bearer " + getAdminToken());
          return null;
        });
    log.info("Deleted user: {}", userId);
  }

  // ============================================================================
  // Role Management
  // ============================================================================

  /** Get all roles */
  public List<KeycloakRoleRepresentation> getRoles() {
    return keycloakCall("getRoles", () -> adminClient.getRoles(realm, "Bearer " + getAdminToken()));
  }

  /** Get user's roles */
  public List<KeycloakRoleRepresentation> getUserRoles(String userId) {
    return keycloakCall(
        "getUserRoles",
        () -> adminClient.getUserRoles(realm, userId, "Bearer " + getAdminToken()));
  }

  /** Assign roles to user */
  public void assignRolesToUser(String userId, List<KeycloakRoleRepresentation> roles) {
    keycloakCall(
        "assignRolesToUser",
        () -> {
          adminClient.assignRoles(realm, userId, roles, "Bearer " + getAdminToken());
          return null;
        });
    log.info("Assigned {} roles to user: {}", roles.size(), userId);
  }

  /** Remove roles from user */
  public void removeRolesFromUser(String userId, List<KeycloakRoleRepresentation> roles) {
    keycloakCall(
        "removeRolesFromUser",
        () -> {
          adminClient.removeRoles(realm, userId, roles, "Bearer " + getAdminToken());
          return null;
        });
    log.info("Removed {} roles from user: {}", roles.size(), userId);
  }

  // ============================================================================
  // Session Management
  // ============================================================================

  /** Logout user (terminate all sessions) */
  public void logoutUser(String userId) {
    keycloakCall(
        "logoutUser",
        () -> {
          adminClient.logoutUser(realm, userId, "Bearer " + getAdminToken());
          return null;
        });
    log.info("Logged out user: {}", userId);
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

  /**
   * Run an external Keycloak call through the shared circuit breaker (ADR-0010). Exceptions
   * propagate to callers — the JWT validation chain and KeycloakErrorDecoder own the fallback —
   * while the breaker records the failure and opens, so a sick Keycloak is rejected fast instead
   * of hanging request threads call after call.
   */
  private <T> T keycloakCall(String operation, Supplier<T> call) {
    log.debug("Keycloak call via circuit breaker: {}", operation);
    return circuitBreakerService.execute("keycloak", call);
  }

  /** Get admin token with caching In production, use Spring Cache or Redis */
  private String getAdminToken() {
    // Check if cached token is still valid
    if (cachedAdminToken != null && System.currentTimeMillis() < tokenExpiryTime) {
      return cachedAdminToken;
    }

    // Get new admin token using service account
    log.info("Getting new admin token");
    KeycloakTokenResponse response =
        tokenClient.getServiceAccountToken(realm, clientId, clientSecret, "client_credentials");

    cachedAdminToken = response.getAccessToken();
    // Set expiry time to 90% of actual expiry (add buffer)
    tokenExpiryTime = System.currentTimeMillis() + (response.getExpiresIn() * 900L);

    return cachedAdminToken;
  }
}
