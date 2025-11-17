package com.pacific.core.service;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/** Shared cache service for all microservices */
@Slf4j
@Service
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true", matchIfMissing = true)
public class CacheService {

  private final CacheManager cacheManager;
  private final RedisTemplate<String, Object> redisTemplate;

  public CacheService(CacheManager cacheManager, RedisTemplate<String, Object> redisTemplate) {
    this.cacheManager = cacheManager;
    this.redisTemplate = redisTemplate;
  }

  /** Get value from cache */
  public <T> T get(String cacheName, String key, Class<T> type) {
    Cache cache = cacheManager.getCache(cacheName);
    if (cache != null) {
      Cache.ValueWrapper wrapper = cache.get(key);
      if (wrapper != null) {
        return type.cast(wrapper.get());
      }
    }
    return null;
  }

  /** Put value in cache */
  public void put(String cacheName, String key, Object value) {
    Cache cache = cacheManager.getCache(cacheName);
    if (cache != null) {
      cache.put(key, value);
      log.debug("Cached value for key: {} in cache: {}", key, cacheName);
    }
  }

  /** Evict value from cache */
  public void evict(String cacheName, String key) {
    Cache cache = cacheManager.getCache(cacheName);
    if (cache != null) {
      cache.evict(key);
      log.debug("Evicted value for key: {} from cache: {}", key, cacheName);
    }
  }

  /** Clear entire cache */
  public void clear(String cacheName) {
    Cache cache = cacheManager.getCache(cacheName);
    if (cache != null) {
      cache.clear();
      log.info("Cleared cache: {}", cacheName);
    }
  }

  /** Clear all caches */
  public void clearAll() {
    Collection<String> cacheNames = cacheManager.getCacheNames();
    for (String cacheName : cacheNames) {
      clear(cacheName);
    }
    log.info("Cleared all caches");
  }

  /** Redis operations */
  public void setRedisValue(String key, Object value, long timeout, TimeUnit unit) {
    redisTemplate.opsForValue().set(key, value, timeout, unit);
    log.debug("Set Redis value for key: {} with timeout: {} {}", key, timeout, unit);
  }

  public Object getRedisValue(String key) {
    return redisTemplate.opsForValue().get(key);
  }

  public void deleteRedisValue(String key) {
    redisTemplate.delete(key);
    log.debug("Deleted Redis value for key: {}", key);
  }

  public void deleteRedisPattern(String pattern) {
    Set<String> keys = redisTemplate.keys(pattern);
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
      log.info("Deleted {} Redis keys matching pattern: {}", keys.size(), pattern);
    }
  }

  /** Get cache statistics */
  public CacheStatistics getCacheStatistics() {
    Collection<String> cacheNames = cacheManager.getCacheNames();
    int totalCaches = cacheNames.size();

    Set<String> redisKeys = redisTemplate.keys("*");
    int redisKeyCount = redisKeys != null ? redisKeys.size() : 0;

    return CacheStatistics.builder()
        .totalCaches(totalCaches)
        .cacheNames(cacheNames)
        .redisKeyCount(redisKeyCount)
        .build();
  }

  /** Get cache health statistics for monitoring */
  public CacheHealthStats getStats() {
    try {
      // Get Redis connection info
      var connection = redisTemplate.getConnectionFactory().getConnection();
      long redisKeys = 0;
      try {
        redisKeys = connection.dbSize();
      } finally {
        connection.close();
      }

      // Get local cache names
      var cacheNames = new java.util.HashSet<>(cacheManager.getCacheNames());

      return new CacheHealthStats(redisKeys, "N/A", cacheNames);
    } catch (Exception e) {
      log.warn("Failed to get cache statistics: {}", e.getMessage());
      return new CacheHealthStats(0, "Error", java.util.Collections.emptySet());
    }
  }

  /** Cache health statistics for monitoring */
  public static class CacheHealthStats {
    private final long redisKeys;
    private final String redisMemoryUsage;
    private final java.util.Set<String> localCacheNames;

    public CacheHealthStats(
        long redisKeys, String redisMemoryUsage, java.util.Set<String> localCacheNames) {
      this.redisKeys = redisKeys;
      this.redisMemoryUsage = redisMemoryUsage;
      this.localCacheNames = localCacheNames;
    }

    public long getRedisKeys() {
      return redisKeys;
    }

    public String getRedisMemoryUsage() {
      return redisMemoryUsage;
    }

    public java.util.Set<String> getLocalCacheNames() {
      return localCacheNames;
    }
  }

  @lombok.Data
  @lombok.Builder
  public static class CacheStatistics {
    private int totalCaches;
    private Collection<String> cacheNames;
    private int redisKeyCount;
  }
}
