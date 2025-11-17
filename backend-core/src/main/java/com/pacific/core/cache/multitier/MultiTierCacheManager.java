package com.pacific.core.cache.multitier;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * Multi-tier cache manager that combines Caffeine (L1) and Redis (L2) caches. Provides a unified
 * interface for accessing both cache layers.
 */
public class MultiTierCacheManager implements CacheManager {

  private CacheManager caffeineCacheManager;
  private CacheManager redisCacheManager;
  private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

  public void setCaffeineCacheManager(CacheManager caffeineCacheManager) {
    this.caffeineCacheManager = caffeineCacheManager;
  }

  public void setRedisCacheManager(CacheManager redisCacheManager) {
    this.redisCacheManager = redisCacheManager;
  }

  @Override
  public Cache getCache(String name) {
    return cacheMap.computeIfAbsent(name, this::createMultiTierCache);
  }

  @Override
  public Collection<String> getCacheNames() {
    return cacheMap.keySet();
  }

  private Cache createMultiTierCache(String name) {
    Cache l1Cache = caffeineCacheManager != null ? caffeineCacheManager.getCache(name) : null;
    Cache l2Cache = redisCacheManager != null ? redisCacheManager.getCache(name) : null;

    return new MultiTierCache(name, l1Cache, l2Cache);
  }
}
