package com.pacific.auth.modules.cache.controller;

import com.pacific.auth.config.AuthCacheConfiguration;
import com.pacific.auth.modules.cache.dto.*;
import com.pacific.auth.modules.cache.service.AuthCacheService;
import com.pacific.core.service.CacheManagementService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Cache module controller for auth-service. Handles cache management operations including stats,
 * reload, and clear. Delegates infrastructure operations to CacheManagementService from
 * backend-core.
 */
@Slf4j
@RestController
@RequestMapping("/api/cache")
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class CacheController {

  private final AuthCacheService authCacheService;
  private final CacheManagementService cacheManagementService;
  private final AuthCacheConfiguration authCacheConfiguration;

  /** Get comprehensive cache statistics. Delegates to CacheManagementService from backend-core. */
  @GetMapping("/stats")
  public ResponseEntity<CacheStatsResponse> getCacheStats() {
    log.info("📊 Getting cache statistics");
    var stats = cacheManagementService.getCacheStatistics();

    return ResponseEntity.ok(
        CacheStatsResponse.builder()
            .totalCaches(stats.getTotalCaches())
            .cacheNames(stats.getCacheNames())
            .redisKeyCount(stats.getRedisKeyCount())
            .build());
  }

  /** Get auth-specific cache statistics. */
  @GetMapping("/stats/auth")
  public ResponseEntity<Map<String, Object>> getAuthCacheStats() {
    log.info("📊 Getting auth-specific cache statistics");

    Map<String, Object> stats = new HashMap<>();
    stats.put("userCacheStats", authCacheService.getUserCacheStats());
    stats.put("roleCacheStats", authCacheService.getRoleCacheStats());
    stats.put("tokenCacheStats", authCacheService.getTokenCacheStats());

    return ResponseEntity.ok(stats);
  }

  /** Check if a specific cache exists. */
  @GetMapping("/exists/{cacheName}")
  public ResponseEntity<CacheExistenceResponse> checkCacheExistence(
      @PathVariable String cacheName) {
    log.info("🔍 Checking cache existence: {}", cacheName);
    boolean exists = authCacheService.cacheExists(cacheName);

    return ResponseEntity.ok(
        CacheExistenceResponse.builder().exists(exists).cacheName(cacheName).build());
  }

  /** Check existence of all auth caches. */
  @GetMapping("/exists/auth/all")
  public ResponseEntity<Map<String, Boolean>> checkAllAuthCaches() {
    log.info("🔍 Checking all auth cache existence");

    Map<String, Boolean> existence = new HashMap<>();
    existence.put("userCache", authCacheService.userCacheExists());
    existence.put("roleCache", authCacheService.roleCacheExists());
    existence.put("tokenCache", authCacheService.tokenCacheExists());
    existence.put("permissionCache", authCacheService.permissionCacheExists());

    return ResponseEntity.ok(existence);
  }

  /** Reload a specific cache by name. */
  @PostMapping("/reload/{cacheName}")
  public ResponseEntity<CacheOperationResponse> reloadCache(@PathVariable String cacheName) {
    log.info("🔄 Reloading cache: {}", cacheName);

    String message;
    switch (cacheName.toLowerCase()) {
      case "users":
        authCacheService.reloadUserCache();
        message = "User cache reloaded successfully";
        break;
      case "roles":
        authCacheService.reloadRoleCache();
        message = "Role cache reloaded successfully";
        break;
      default:
        authCacheService.clear(cacheName);
        message = "Cache cleared: " + cacheName;
    }

    return ResponseEntity.ok(
        CacheOperationResponse.builder()
            .status("success")
            .message(message)
            .cacheName(cacheName)
            .build());
  }

  /** Reload all auth caches. */
  @PostMapping("/reload/auth/all")
  public ResponseEntity<CacheOperationResponse> reloadAuthCaches() {
    log.info("🔄 Reloading all auth caches");
    authCacheConfiguration.reloadAuthCaches();

    return ResponseEntity.ok(
        CacheOperationResponse.builder()
            .status("success")
            .message("All auth caches reloaded successfully")
            .cacheName("all")
            .build());
  }

  /** Clear a specific cache. Delegates to CacheManagementService from backend-core. */
  @DeleteMapping("/clear/{cacheName}")
  public ResponseEntity<CacheOperationResponse> clearCache(@PathVariable String cacheName) {
    log.info("🗑️ Clearing cache: {}", cacheName);
    cacheManagementService.clearCache(cacheName);

    return ResponseEntity.ok(
        CacheOperationResponse.builder()
            .status("success")
            .message("Cache cleared successfully")
            .cacheName(cacheName)
            .build());
  }

  /** Clear all caches. Delegates to CacheManagementService from backend-core. */
  @DeleteMapping("/clear/all")
  public ResponseEntity<CacheOperationResponse> clearAllCaches() {
    log.info("🗑️ Clearing all caches");
    cacheManagementService.clearAllCaches();

    return ResponseEntity.ok(
        CacheOperationResponse.builder()
            .status("success")
            .message("All caches cleared successfully")
            .cacheName("all")
            .build());
  }

  /** Clear all auth-specific caches. */
  @DeleteMapping("/clear/auth/all")
  public ResponseEntity<CacheOperationResponse> clearAllAuthCaches() {
    log.info("🗑️ Clearing all auth caches");
    authCacheConfiguration.clearAuthCaches();

    return ResponseEntity.ok(
        CacheOperationResponse.builder()
            .status("success")
            .message("All auth caches cleared successfully")
            .cacheName("auth-all")
            .build());
  }

  /**
   * Test cache serialization. FOR DEVELOPMENT/TESTING ONLY - Not available in production. Used for
   * testing cache storage and retrieval with sample data.
   */
  @PostMapping("/test-serialization")
  @Profile({"dev", "test"})
  public ResponseEntity<Map<String, Object>> testCacheSerialization() {
    log.info("Testing cache serialization (dev/test only)");

    Map<String, Object> response = new HashMap<>();

    // NOTE: This uses hardcoded test data for development/testing purposes only
    // Test user cache serialization
    String userId = "test-user-123";
    Map<String, Object> userData = new HashMap<>();
    userData.put("id", userId);
    userData.put("username", "testuser");
    userData.put("email", "test@example.com");
    userData.put("roles", new String[] {"USER", "ADMIN"});

    authCacheService.putUserInCache(userId, userData);

    // Test role cache serialization
    String roleId = "test-role-456";
    Map<String, Object> roleData = new HashMap<>();
    roleData.put("id", roleId);
    roleData.put("name", "TEST_ROLE");
    roleData.put("permissions", new String[] {"READ", "WRITE"});

    authCacheService.putRoleInCache(roleId, roleData);

    response.put("message", "Cache serialization test completed");
    response.put("userData", userData);
    response.put("roleData", roleData);
    response.put("warning", "This endpoint is for development/testing only");

    return ResponseEntity.ok(response);
  }

  /** Health check for cache system. */
  @GetMapping("/health")
  public ResponseEntity<CacheHealthResponse> cacheHealth() {
    log.info("🏥 Checking cache health");

    return ResponseEntity.ok(
        CacheHealthResponse.builder()
            .status("UP")
            .userCacheExists(authCacheService.userCacheExists())
            .roleCacheExists(authCacheService.roleCacheExists())
            .tokenCacheExists(authCacheService.tokenCacheExists())
            .permissionCacheExists(authCacheService.permissionCacheExists())
            .build());
  }
}
