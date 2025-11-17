package com.pacific.core.service;

import java.time.Duration;
import java.util.concurrent.Callable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * Base cache service with single responsibility. Provides common cache operations that can be
 * extended by services.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseCacheService {

  protected final CacheManager cacheManager;

  /** Get value from cache. Single responsibility: Retrieve cached value. */
  public <T> T get(String cacheName, Object key, Class<T> type) {
    Cache cache = cacheManager.getCache(cacheName);
    if (cache != null) {
      Cache.ValueWrapper wrapper = cache.get(key);
      if (wrapper != null) {
        return type.cast(wrapper.get());
      }
    }
    return null;
  }

  /**
   * Get value from cache with value loader. Single responsibility: Retrieve cached value or load if
   * not present.
   */
  public <T> T get(String cacheName, Object key, Callable<T> valueLoader) {
    Cache cache = cacheManager.getCache(cacheName);
    if (cache != null) {
      try {
        return cache.get(key, valueLoader);
      } catch (Exception e) {
        log.error("Error loading cache value for key: {}", key, e);
        throw new RuntimeException("Error loading cache value", e);
      }
    }
    return null;
  }

  /** Put value into cache. Single responsibility: Store value in cache. */
  public void put(String cacheName, Object key, Object value) {
    Cache cache = cacheManager.getCache(cacheName);
    if (cache != null) {
      cache.put(key, value);
      log.debug("Cached value for key: {} in cache: {}", key, cacheName);
    }
  }

  /** Evict value from cache. Single responsibility: Remove value from cache. */
  public void evict(String cacheName, Object key) {
    Cache cache = cacheManager.getCache(cacheName);
    if (cache != null) {
      cache.evict(key);
      log.debug("Evicted value for key: {} from cache: {}", key, cacheName);
    }
  }

  /** Clear entire cache. Single responsibility: Clear all values from cache. */
  public void clear(String cacheName) {
    Cache cache = cacheManager.getCache(cacheName);
    if (cache != null) {
      cache.clear();
      log.info("Cleared cache: {}", cacheName);
    }
  }

  /** Check if cache exists. Single responsibility: Check cache existence. */
  public boolean cacheExists(String cacheName) {
    return cacheManager.getCache(cacheName) != null;
  }

  /** Get cache statistics. Single responsibility: Provide cache statistics. */
  public Object getCacheStats(String cacheName) {
    Cache cache = cacheManager.getCache(cacheName);
    if (cache != null
        && cache.getNativeCache() instanceof com.github.benmanes.caffeine.cache.Cache) {
      com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineCache =
          (com.github.benmanes.caffeine.cache.Cache<?, ?>) cache.getNativeCache();
      return caffeineCache.stats();
    }
    return null;
  }

  /**
   * Abstract method for service-specific cache configuration. Single responsibility: Define
   * service-specific cache behavior.
   */
  protected abstract String getServiceName();

  /**
   * Abstract method for service-specific cache TTL. Single responsibility: Define service-specific
   * cache TTL.
   */
  protected abstract Duration getDefaultTtl();
}
