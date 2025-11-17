package com.pacific.core.service;

import java.util.Collection;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/** Centralized cache management service for all microservices */
@Slf4j
@Service
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true", matchIfMissing = true)
public class CacheManagementService {

  private final CacheManager cacheManager;
  private final RedisTemplate<String, Object> redisTemplate;

  public CacheManagementService(
      CacheManager cacheManager, RedisTemplate<String, Object> redisTemplate) {
    this.cacheManager = cacheManager;
    this.redisTemplate = redisTemplate;
  }

  /** Clear all caches */
  public void clearAllCaches() {
    log.info("Clearing all caches");

    Collection<String> cacheNames = cacheManager.getCacheNames();
    for (String cacheName : cacheNames) {
      Cache cache = cacheManager.getCache(cacheName);
      if (cache != null) {
        cache.clear();
        log.info("Cleared cache: {}", cacheName);
      }
    }

    // Clear Redis cache
    Set<String> keys = redisTemplate.keys("*");
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
      log.info("Cleared {} Redis keys", keys.size());
    }

    // Event publishing removed to maintain backend-core independence
  }

  /** Clear specific cache by name */
  public void clearCache(String cacheName) {
    log.info("Clearing cache: {}", cacheName);

    Cache cache = cacheManager.getCache(cacheName);
    if (cache != null) {
      cache.clear();
      log.info("Cleared cache: {}", cacheName);

      // Event publishing removed to maintain backend-core independence
    } else {
      log.warn("Cache not found: {}", cacheName);
    }
  }

  /** Clear Redis cache by pattern */
  public void clearRedisCache(String pattern) {
    log.info("Clearing Redis cache with pattern: {}", pattern);

    Set<String> keys = redisTemplate.keys(pattern);
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
      log.info("Cleared {} Redis keys matching pattern: {}", keys.size(), pattern);
    } else {
      log.info("No Redis keys found matching pattern: {}", pattern);
    }
  }

  /** Get cache statistics */
  public CacheStatistics getCacheStatistics() {
    Collection<String> cacheNames = cacheManager.getCacheNames();
    int totalCaches = cacheNames.size();

    // Get Redis statistics
    Set<String> redisKeys = redisTemplate.keys("*");
    int redisKeyCount = redisKeys != null ? redisKeys.size() : 0;

    return CacheStatistics.builder()
        .totalCaches(totalCaches)
        .cacheNames(cacheNames)
        .redisKeyCount(redisKeyCount)
        .build();
  }

  /** Cache statistics DTO */
  @lombok.Data
  @lombok.Builder
  public static class CacheStatistics {
    private int totalCaches;
    private Collection<String> cacheNames;
    private int redisKeyCount;
  }
}
