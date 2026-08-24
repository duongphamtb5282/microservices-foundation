package com.pacific.auth.modules.user.controller;

import com.pacific.auth.modules.user.dto.response.UserInfoDto;
import com.pacific.auth.modules.user.service.UserService;
import com.pacific.core.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * User information controller with advanced caching strategies. Unexpected exceptions propagate to
 * {@link com.pacific.auth.common.exception.GlobalExceptionHandler} — no inline catch-all blocks.
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User information and management endpoints")
public class UserController {

  private final UserService userService;

  @Autowired(required = false)
  private CacheService cacheService;

  /**
   * Get current user information with multi-layer caching Note: Caching is handled at the service
   * layer to cache UserInfoDto directly, not the ResponseEntity wrapper. This ensures proper
   * serialization to Redis.
   */
  @Operation(
      summary = "Get current user info",
      description = "Retrieves current user information with advanced caching strategy",
      security = @SecurityRequirement(name = "bearerAuth"))
  @GetMapping("/me")
  public ResponseEntity<UserInfoDto> getCurrentUserInfo(Authentication authentication) {
    log.info("🔍 Retrieving user info for: {}", authentication.getName());

    UserInfoDto userInfo = userService.getCurrentUserInfo(authentication.getName());
    log.info("✅ User info retrieved successfully for: {}", userInfo);
    return ResponseEntity.ok(userInfo);
  }

  /** Get user by ID with caching */
  @Operation(
      summary = "Get user by ID",
      description = "Retrieves user information by ID with caching",
      security = @SecurityRequirement(name = "bearerAuth"))
  @GetMapping("/{userId}")
  public ResponseEntity<UserInfoDto> getUserById(@PathVariable String userId) {
    log.info("🔍 Retrieving user by ID: {}", userId);

    Optional<UserInfoDto> userInfo = userService.getUserById(userId);
    if (userInfo.isPresent()) {
      log.info("✅ User found: {}", userId);
      return ResponseEntity.ok(userInfo.get());
    }
    log.warn("⚠️ User not found: {}", userId);
    return ResponseEntity.notFound().build();
  }

  /** Get user by username with caching */
  @Operation(
      summary = "Get user by username",
      description = "Retrieves user information by username with caching",
      security = @SecurityRequirement(name = "bearerAuth"))
  @GetMapping("/username/{username}")
  @Cacheable(value = "user-by-username", key = "#username")
  public ResponseEntity<UserInfoDto> getUserByUsername(@PathVariable String username) {
    log.info("🔍 Retrieving user by username: {}", username);

    Optional<UserInfoDto> userInfo = userService.getUserByUsername(username);
    if (userInfo.isPresent()) {
      log.info("✅ User found by username: {}", username);
      return ResponseEntity.ok(userInfo.get());
    }
    log.warn("⚠️ User not found by username: {}", username);
    return ResponseEntity.notFound().build();
  }

  /** Get all users with pagination and caching */
  @Operation(
      summary = "Get all users",
      description = "Retrieves all users with pagination and caching",
      security = @SecurityRequirement(name = "bearerAuth"))
  @GetMapping
  @Cacheable(value = "all-users", key = "#page + '-' + #size")
  public ResponseEntity<Map<String, Object>> getAllUsers(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    log.info("🔍 Retrieving all users - page: {}, size: {}", page, size);

    Map<String, Object> result = userService.getAllUsers(page, size);
    log.info("✅ Retrieved {} users", result.get("totalElements"));
    return ResponseEntity.ok(result);
  }

  /** Update user information with cache invalidation */
  @Operation(
      summary = "Update user info",
      description = "Updates user information and invalidates related caches",
      security = @SecurityRequirement(name = "bearerAuth"))
  @PutMapping("/{userId}")
  @CachePut(value = "user-by-id", key = "#userId")
  @CacheEvict(
      value = {"user-info", "user-by-username", "all-users"},
      allEntries = true)
  public ResponseEntity<UserInfoDto> updateUser(
      @PathVariable String userId, @RequestBody Map<String, Object> updates) {
    log.info("🔄 Updating user: {} with updates: {}", userId, updates);

    UserInfoDto updatedUser = userService.updateUser(userId, updates);
    log.info("✅ User updated successfully: {}", userId);
    return ResponseEntity.ok(updatedUser);
  }

  /** Delete user with cache invalidation */
  @Operation(
      summary = "Delete user",
      description = "Deletes user and invalidates related caches",
      security = @SecurityRequirement(name = "bearerAuth"))
  @DeleteMapping("/{userId}")
  @CacheEvict(
      value = {"user-info", "user-by-id", "user-by-username", "all-users"},
      allEntries = true)
  public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String userId) {
    log.info("🗑️ Deleting user: {}", userId);

    userService.deleteUser(userId);
    log.info("✅ User deleted successfully: {}", userId);
    return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
  }

  /** Search users with caching */
  @Operation(
      summary = "Search users",
      description = "Searches users by criteria with caching",
      security = @SecurityRequirement(name = "bearerAuth"))
  @GetMapping("/search")
  @Cacheable(value = "user-search", key = "#query + '-' + #page + '-' + #size")
  public ResponseEntity<Map<String, Object>> searchUsers(
      @RequestParam String query,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    log.info("🔍 Searching users with query: '{}', page: {}, size: {}", query, page, size);

    Map<String, Object> result = userService.searchUsers(query, page, size);
    log.info("✅ Found {} users matching query: '{}'", result.get("totalElements"), query);
    return ResponseEntity.ok(result);
  }

  /** Get user roles with caching */
  @Operation(
      summary = "Get user roles",
      description = "Retrieves user roles with caching",
      security = @SecurityRequirement(name = "bearerAuth"))
  @GetMapping("/{userId}/roles")
  @Cacheable(value = "user-roles", key = "#userId")
  public ResponseEntity<Map<String, Object>> getUserRoles(@PathVariable String userId) {
    log.info("🔍 Retrieving roles for user: {}", userId);

    Map<String, Object> roles = userService.getUserRoles(userId);
    log.info("✅ Retrieved {} roles for user: {}", roles.get("roles"), userId);
    return ResponseEntity.ok(roles);
  }

  /** Cache management endpoints */
  @Operation(
      summary = "Clear user cache",
      description = "Clears user-related caches",
      security = @SecurityRequirement(name = "bearerAuth"))
  @PostMapping("/cache/clear")
  public ResponseEntity<Map<String, String>> clearUserCache() {
    log.info("🧹 Clearing user cache");

    cacheService.clear("user-info");
    cacheService.clear("user-by-id");
    cacheService.clear("user-by-username");
    cacheService.clear("all-users");
    cacheService.clear("user-search");
    cacheService.clear("user-roles");

    log.info("✅ User cache cleared successfully");
    return ResponseEntity.ok(Map.of("message", "User cache cleared successfully"));
  }

  /** Get cache statistics for user endpoints */
  @Operation(
      summary = "Get user cache stats",
      description = "Retrieves cache statistics for user endpoints",
      security = @SecurityRequirement(name = "bearerAuth"))
  @GetMapping("/cache/stats")
  public ResponseEntity<Map<String, Object>> getUserCacheStats() {
    log.info("📊 Retrieving user cache statistics");

    Map<String, Object> stats = new HashMap<>();
    stats.put("user-info", cacheService.getCacheStatistics());
    stats.put("user-by-id", cacheService.getCacheStatistics());
    stats.put("user-by-username", cacheService.getCacheStatistics());
    stats.put("all-users", cacheService.getCacheStatistics());
    stats.put("user-search", cacheService.getCacheStatistics());
    stats.put("user-roles", cacheService.getCacheStatistics());

    log.info("✅ User cache statistics retrieved");
    return ResponseEntity.ok(stats);
  }
}
