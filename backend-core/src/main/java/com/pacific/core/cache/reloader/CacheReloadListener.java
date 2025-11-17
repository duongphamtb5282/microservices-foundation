package com.pacific.core.cache.reloader;

/**
 * Interface for cache reload listeners. Implementations can be notified when caches need to be
 * reloaded.
 */
public interface CacheReloadListener {

  /**
   * Called when a cache needs to be reloaded.
   *
   * @param cacheName the name of the cache that needs reloading
   */
  void onCacheReload(String cacheName);

  /** Called when all caches need to be reloaded. */
  void onAllCachesReload();
}
