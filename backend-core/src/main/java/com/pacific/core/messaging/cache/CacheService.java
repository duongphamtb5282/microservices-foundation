package com.pacific.core.messaging.cache;

import java.time.Duration;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for caching operations using Redis and local cache. Provides multi-level caching with
 * Redis as primary and local cache as fallback.
 */
@Service("messagingCacheService")
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class CacheService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final CacheManager cacheManager;

  /** Get value from cache (Redis first, then local cache). */
  @SuppressWarnings("unchecked")
  public <T> Optional<T> get(String key, Class<T> type) {
    try {
      // Try Redis first
      Object redisValue = redisTemplate.opsForValue().get(key);
      if (redisValue != null) {
        log.debug("Cache hit in Redis for key: {}", key);
        return Optional.of((T) redisValue);
      }

      // Try local cache
      org.springframework.cache.Cache cache = cacheManager.getCache("local");
      if (cache != null) {
        org.springframework.cache.Cache.ValueWrapper localValue = cache.get(key);
        if (localValue != null) {
          log.debug("Cache hit in local cache for key: {}", key);
          return Optional.of((T) localValue.get());
        }
      }

      log.debug("Cache miss for key: {}", key);
      return Optional.empty();

    } catch (Exception e) {
      log.warn("Error getting from cache for key: {}", key, e);
      return Optional.empty();
    }
  }

  /** Put value in cache (both Redis and local cache). */
  public void put(String key, Object value, Duration ttl) {
    try {
      // Put in Redis
      redisTemplate.opsForValue().set(key, value, ttl);

      // Put in local cache with shorter TTL
      org.springframework.cache.Cache cache = cacheManager.getCache("local");
      if (cache != null) {
        cache.put(key, value);
      }

      log.debug("Cached value for key: {} with TTL: {}", key, ttl);

    } catch (Exception e) {
      log.warn("Error putting to cache for key: {}", key, e);
    }
  }

  /** Remove value from cache. */
  public void evict(String key) {
    try {
      // Remove from Redis
      redisTemplate.delete(key);

      // Remove from local cache
      org.springframework.cache.Cache cache = cacheManager.getCache("local");
      if (cache != null) {
        cache.evict(key);
      }

      log.debug("Evicted cache for key: {}", key);

    } catch (Exception e) {
      log.warn("Error evicting cache for key: {}", key, e);
    }
  }

  /** Clear all caches. */
  public void clearAll() {
    try {
      // Clear Redis
      redisTemplate.getConnectionFactory().getConnection().flushAll();

      // Clear local caches
      cacheManager
          .getCacheNames()
          .forEach(
              cacheName -> {
                org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                  cache.clear();
                }
              });

      log.info("Cleared all caches");

    } catch (Exception e) {
      log.warn("Error clearing caches", e);
    }
  }

  /** Check if key exists in any cache. */
  public boolean exists(String key) {
    try {
      // Check Redis
      Boolean redisExists = redisTemplate.hasKey(key);
      if (Boolean.TRUE.equals(redisExists)) {
        return true;
      }

      // Check local cache
      org.springframework.cache.Cache cache = cacheManager.getCache("local");
      if (cache != null) {
        return cache.get(key) != null;
      }

      return false;

    } catch (Exception e) {
      log.warn("Error checking cache existence for key: {}", key, e);
      return false;
    }
  }

  /** Get cache statistics. */
  public CacheStats getStats() {
    try {
      // Redis stats
      var connection = redisTemplate.getConnectionFactory().getConnection();
      java.util.Properties info = connection.info("memory");

      return CacheStats.builder()
          .redisKeys(redisTemplate.getConnectionFactory().getConnection().dbSize())
          .redisMemoryUsage(info.getProperty("used_memory_human", "unknown"))
          .localCacheNames(cacheManager.getCacheNames().size())
          .build();

    } catch (Exception e) {
      log.warn("Error getting cache stats", e);
      return CacheStats.builder()
          .redisKeys(0L)
          .redisMemoryUsage("error")
          .localCacheNames(0)
          .build();
    }
  }

  /** Cache statistics. */
  public static class CacheStats {
    private final long redisKeys;
    private final String redisMemoryUsage;
    private final int localCacheNames;

    public CacheStats(long redisKeys, String redisMemoryUsage, int localCacheNames) {
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

    public int getLocalCacheNames() {
      return localCacheNames;
    }

    public static CacheStatsBuilder builder() {
      return new CacheStatsBuilder();
    }

    public static class CacheStatsBuilder {
      private long redisKeys;
      private String redisMemoryUsage;
      private int localCacheNames;

      public CacheStatsBuilder redisKeys(long redisKeys) {
        this.redisKeys = redisKeys;
        return this;
      }

      public CacheStatsBuilder redisMemoryUsage(String redisMemoryUsage) {
        this.redisMemoryUsage = redisMemoryUsage;
        return this;
      }

      public CacheStatsBuilder localCacheNames(int localCacheNames) {
        this.localCacheNames = localCacheNames;
        return this;
      }

      public CacheStats build() {
        return new CacheStats(redisKeys, redisMemoryUsage, localCacheNames);
      }
    }
  }
}
