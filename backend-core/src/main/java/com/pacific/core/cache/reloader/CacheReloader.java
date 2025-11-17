package com.pacific.core.cache.reloader;

import java.util.Collection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/** Service for reloading caches when they expire or need to be refreshed. */
@Component
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class CacheReloader {

  private final CacheManager cacheManager;

  public CacheReloader(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  /** Reload all caches by clearing them. */
  public void reloadAllCaches() {
    log.info("🔄 Reloading all caches");

    Collection<String> cacheNames = cacheManager.getCacheNames();
    for (String cacheName : cacheNames) {
      cacheManager.getCache(cacheName).clear();
      log.debug("Cleared cache: {}", cacheName);
    }

    log.info("✅ All caches reloaded successfully");
  }

  /** Reload a specific cache by clearing it. */
  public void reloadCache(String cacheName) {
    log.info("🔄 Reloading cache: {}", cacheName);

    if (cacheManager.getCache(cacheName) != null) {
      cacheManager.getCache(cacheName).clear();
      log.info("✅ Cache '{}' reloaded successfully", cacheName);
    } else {
      log.warn("Cache '{}' not found", cacheName);
    }
  }
}
