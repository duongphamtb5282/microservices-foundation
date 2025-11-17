package com.pacific.auth.modules.cache.service;

import com.pacific.core.service.BaseCacheService;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Auth-service specific cache service. Extends BaseCacheService with auth-specific cache
 * operations. Single responsibility: Handle auth-specific caching.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true", matchIfMissing = true)
public class AuthCacheService extends BaseCacheService {

  @Value("${auth-service.auth-cache.user-ttl:1h}")
  private String userTtl;

  public AuthCacheService(CacheManager cacheManager) {
    super(cacheManager);
  }

  @Override
  protected String getServiceName() {
    return "auth-service";
  }

  @Override
  protected Duration getDefaultTtl() {
    return Duration.parse("PT" + userTtl.toUpperCase().replace("H", "H").replace("M", "M"));
  }

  /** Cache user data. Single responsibility: Cache user information. */
  public void cacheUser(String userId, Object user) {
    put("users", userId, user);
    log.debug("Cached user: {}", userId);
  }

  /** Get user from cache. Single responsibility: Retrieve cached user. */
  public Object getUserFromCache(String userId) {
    return get("users", userId, Object.class);
  }

  /** Cache token data. Single responsibility: Cache token information. */
  public void cacheToken(String token, Object tokenData) {
    put("tokens", token, tokenData);
    log.debug("Cached token");
  }

  /** Get token from cache. Single responsibility: Retrieve cached token. */
  public Object getTokenFromCache(String token) {
    return get("tokens", token, Object.class);
  }

  /** Cache role data. Single responsibility: Cache role information. */
  public void cacheRole(String roleId, Object role) {
    put("roles", roleId, role);
    log.debug("Cached role: {}", roleId);
  }

  /** Get role from cache. Single responsibility: Retrieve cached role. */
  public Object getRoleFromCache(String roleId) {
    return get("roles", roleId, Object.class);
  }

  /** Cache permission data. Single responsibility: Cache permission information. */
  public void cachePermission(String userId, String resource, Object permission) {
    String key = userId + "_" + resource;
    put("permissions", key, permission);
    log.debug("Cached permission for user: {} resource: {}", userId, resource);
  }

  /** Get permission from cache. Single responsibility: Retrieve cached permission. */
  public Object getPermissionFromCache(String userId, String resource) {
    String key = userId + "_" + resource;
    return get("permissions", key, Object.class);
  }

  /** Clear user cache. Single responsibility: Clear user cache. */
  public void clearUserCache() {
    clear("users");
    log.info("Cleared user cache");
  }

  /** Clear token cache. Single responsibility: Clear token cache. */
  public void clearTokenCache() {
    clear("tokens");
    log.info("Cleared token cache");
  }

  /** Clear role cache. Single responsibility: Clear role cache. */
  public void clearRoleCache() {
    clear("roles");
    log.info("Cleared role cache");
  }

  /** Clear permission cache. Single responsibility: Clear permission cache. */
  public void clearPermissionCache() {
    clear("permissions");
    log.info("Cleared permission cache");
  }

  // Cache stats methods
  public Object getUserCacheStats() {
    return getCacheStats("users");
  }

  public Object getRoleCacheStats() {
    return getCacheStats("roles");
  }

  public Object getTokenCacheStats() {
    return getCacheStats("tokens");
  }

  public Object getPermissionCacheStats() {
    return getCacheStats("permissions");
  }

  // Cache existence methods
  public boolean userCacheExists() {
    return cacheExists("users");
  }

  public boolean roleCacheExists() {
    return cacheExists("roles");
  }

  public boolean tokenCacheExists() {
    return cacheExists("tokens");
  }

  public boolean permissionCacheExists() {
    return cacheExists("permissions");
  }

  // Cache reload methods
  public void reloadUserCache() {
    log.info("Reloading user cache");
    clear("users");
  }

  public void reloadRoleCache() {
    log.info("Reloading role cache");
    clear("roles");
  }

  public void reloadAllAuthCaches() {
    log.info("Reloading all auth caches");
    clear("users");
    clear("roles");
    clear("tokens");
    clear("permissions");
  }

  // Cache put methods
  public void putUserInCache(String userId, Object userData) {
    put("users", userId, userData);
  }

  public void putRoleInCache(String roleId, Object roleData) {
    put("roles", roleId, roleData);
  }
}
