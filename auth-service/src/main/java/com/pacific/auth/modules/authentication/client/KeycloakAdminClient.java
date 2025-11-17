package com.pacific.auth.modules.authentication.client;

import com.pacific.auth.modules.authentication.client.dto.KeycloakRoleRepresentation;
import com.pacific.auth.modules.authentication.client.dto.KeycloakUserRepresentation;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Feign Client for Keycloak Admin API Handles user, role, and group management
 *
 * <p>Note: Requires service account with admin privileges
 */
@ConditionalOnProperty(name = "auth-service.security.keycloak.enabled", havingValue = "true")
@FeignClient(
    name = "keycloak-admin",
    url = "${auth-service.security.keycloak.server-url:http://localhost:8080}",
    configuration = KeycloakFeignConfig.class)
public interface KeycloakAdminClient {

  // ============================================================================
  // User Management
  // ============================================================================

  /** Get all users */
  @GetMapping("/admin/realms/{realm}/users")
  List<KeycloakUserRepresentation> getUsers(
      @PathVariable("realm") String realm, @RequestHeader("Authorization") String bearerToken);

  /** Get user by ID */
  @GetMapping("/admin/realms/{realm}/users/{userId}")
  KeycloakUserRepresentation getUser(
      @PathVariable("realm") String realm,
      @PathVariable("userId") String userId,
      @RequestHeader("Authorization") String bearerToken);

  /** Search users by username */
  @GetMapping("/admin/realms/{realm}/users")
  List<KeycloakUserRepresentation> searchUsers(
      @PathVariable("realm") String realm,
      @RequestParam("username") String username,
      @RequestHeader("Authorization") String bearerToken);

  /** Create user */
  @PostMapping("/admin/realms/{realm}/users")
  ResponseEntity<Void> createUser(
      @PathVariable("realm") String realm,
      @RequestBody KeycloakUserRepresentation user,
      @RequestHeader("Authorization") String bearerToken);

  /** Update user */
  @PutMapping("/admin/realms/{realm}/users/{userId}")
  void updateUser(
      @PathVariable("realm") String realm,
      @PathVariable("userId") String userId,
      @RequestBody KeycloakUserRepresentation user,
      @RequestHeader("Authorization") String bearerToken);

  /** Delete user */
  @DeleteMapping("/admin/realms/{realm}/users/{userId}")
  void deleteUser(
      @PathVariable("realm") String realm,
      @PathVariable("userId") String userId,
      @RequestHeader("Authorization") String bearerToken);

  // ============================================================================
  // Role Management
  // ============================================================================

  /** Get all realm roles */
  @GetMapping("/admin/realms/{realm}/roles")
  List<KeycloakRoleRepresentation> getRoles(
      @PathVariable("realm") String realm, @RequestHeader("Authorization") String bearerToken);

  /** Get user's realm roles */
  @GetMapping("/admin/realms/{realm}/users/{userId}/role-mappings/realm")
  List<KeycloakRoleRepresentation> getUserRoles(
      @PathVariable("realm") String realm,
      @PathVariable("userId") String userId,
      @RequestHeader("Authorization") String bearerToken);

  /** Assign roles to user */
  @PostMapping("/admin/realms/{realm}/users/{userId}/role-mappings/realm")
  void assignRoles(
      @PathVariable("realm") String realm,
      @PathVariable("userId") String userId,
      @RequestBody List<KeycloakRoleRepresentation> roles,
      @RequestHeader("Authorization") String bearerToken);

  /** Remove roles from user */
  @DeleteMapping("/admin/realms/{realm}/users/{userId}/role-mappings/realm")
  void removeRoles(
      @PathVariable("realm") String realm,
      @PathVariable("userId") String userId,
      @RequestBody List<KeycloakRoleRepresentation> roles,
      @RequestHeader("Authorization") String bearerToken);

  // ============================================================================
  // Password Management
  // ============================================================================

  /** Reset user password */
  @PutMapping("/admin/realms/{realm}/users/{userId}/reset-password")
  void resetPassword(
      @PathVariable("realm") String realm,
      @PathVariable("userId") String userId,
      @RequestBody Object credentialRepresentation,
      @RequestHeader("Authorization") String bearerToken);

  /** Send password reset email */
  @PutMapping("/admin/realms/{realm}/users/{userId}/execute-actions-email")
  void sendPasswordResetEmail(
      @PathVariable("realm") String realm,
      @PathVariable("userId") String userId,
      @RequestBody List<String> actions,
      @RequestHeader("Authorization") String bearerToken);

  // ============================================================================
  // Session Management
  // ============================================================================

  /** Logout user (terminate all sessions) */
  @PostMapping("/admin/realms/{realm}/users/{userId}/logout")
  void logoutUser(
      @PathVariable("realm") String realm,
      @PathVariable("userId") String userId,
      @RequestHeader("Authorization") String bearerToken);
}
