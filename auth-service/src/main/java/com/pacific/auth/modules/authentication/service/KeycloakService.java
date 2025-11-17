package com.pacific.auth.modules.authentication.service;

import com.pacific.auth.modules.authentication.client.KeycloakAdminClient;
import com.pacific.auth.modules.authentication.client.KeycloakTokenClient;
import com.pacific.auth.modules.authentication.client.dto.KeycloakRoleRepresentation;
import com.pacific.auth.modules.authentication.client.dto.KeycloakTokenResponse;
import com.pacific.auth.modules.authentication.client.dto.KeycloakUserRepresentation;
import java.util.List;
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
@ConditionalOnProperty(name = "auth-service.security.keycloak.enabled", havingValue = "true")
public class KeycloakService {

  private final KeycloakTokenClient tokenClient;
  private final KeycloakAdminClient adminClient;

  @Value("${auth-service.security.keycloak.realm:master}")
  private String realm;

  @Value("${auth-service.security.keycloak.client-id:auth-service}")
  private String clientId;

  @Value("${auth-service.security.keycloak.client-secret:}")
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
    return tokenClient.getToken(realm, clientId, clientSecret, username, password, "password");
  }

  /** Refresh access token */
  public KeycloakTokenResponse refreshToken(String refreshToken) {
    log.info("Refreshing token");
    return tokenClient.refreshToken(realm, clientId, clientSecret, refreshToken, "refresh_token");
  }

  /** Logout user */
  public void logout(String refreshToken) {
    log.info("Logging out user");
    tokenClient.logout(realm, clientId, clientSecret, refreshToken);
  }

  /** Revoke token */
  public void revokeToken(String token) {
    log.info("Revoking token");
    tokenClient.revokeToken(realm, clientId, clientSecret, token);
  }

  // ============================================================================
  // User Management
  // ============================================================================

  /** Get all users */
  public List<KeycloakUserRepresentation> getUsers() {
    String token = getAdminToken();
    return adminClient.getUsers(realm, "Bearer " + token);
  }

  /** Get user by ID */
  public KeycloakUserRepresentation getUser(String userId) {
    String token = getAdminToken();
    return adminClient.getUser(realm, userId, "Bearer " + token);
  }

  /** Search users by username */
  public List<KeycloakUserRepresentation> searchUsers(String username) {
    String token = getAdminToken();
    return adminClient.searchUsers(realm, username, "Bearer " + token);
  }

  /** Create user */
  public void createUser(KeycloakUserRepresentation user) {
    String token = getAdminToken();
    adminClient.createUser(realm, user, "Bearer " + token);
    log.info("Created user: {}", user.getUsername());
  }

  /** Update user */
  public void updateUser(String userId, KeycloakUserRepresentation user) {
    String token = getAdminToken();
    adminClient.updateUser(realm, userId, user, "Bearer " + token);
    log.info("Updated user: {}", userId);
  }

  /** Delete user */
  public void deleteUser(String userId) {
    String token = getAdminToken();
    adminClient.deleteUser(realm, userId, "Bearer " + token);
    log.info("Deleted user: {}", userId);
  }

  // ============================================================================
  // Role Management
  // ============================================================================

  /** Get all roles */
  public List<KeycloakRoleRepresentation> getRoles() {
    String token = getAdminToken();
    return adminClient.getRoles(realm, "Bearer " + token);
  }

  /** Get user's roles */
  public List<KeycloakRoleRepresentation> getUserRoles(String userId) {
    String token = getAdminToken();
    return adminClient.getUserRoles(realm, userId, "Bearer " + token);
  }

  /** Assign roles to user */
  public void assignRolesToUser(String userId, List<KeycloakRoleRepresentation> roles) {
    String token = getAdminToken();
    adminClient.assignRoles(realm, userId, roles, "Bearer " + token);
    log.info("Assigned {} roles to user: {}", roles.size(), userId);
  }

  /** Remove roles from user */
  public void removeRolesFromUser(String userId, List<KeycloakRoleRepresentation> roles) {
    String token = getAdminToken();
    adminClient.removeRoles(realm, userId, roles, "Bearer " + token);
    log.info("Removed {} roles from user: {}", roles.size(), userId);
  }

  // ============================================================================
  // Session Management
  // ============================================================================

  /** Logout user (terminate all sessions) */
  public void logoutUser(String userId) {
    String token = getAdminToken();
    adminClient.logoutUser(realm, userId, "Bearer " + token);
    log.info("Logged out user: {}", userId);
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

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
