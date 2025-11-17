package com.pacific.core.cache.multitier;

import java.util.concurrent.Callable;

import org.springframework.cache.Cache;

/**
 * Multi-tier cache implementation that combines L1 (Caffeine) and L2 (Redis) caches. Implements a
 * write-through strategy where writes go to both caches.
 */
public class MultiTierCache implements Cache {

  private final String name;
  private final Cache l1Cache; // Caffeine
  private final Cache l2Cache; // Redis

  public MultiTierCache(String name, Cache l1Cache, Cache l2Cache) {
    this.name = name;
    this.l1Cache = l1Cache;
    this.l2Cache = l2Cache;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Object getNativeCache() {
    return this;
  }

  @Override
  public ValueWrapper get(Object key) {
    // Try L1 cache first
    if (l1Cache != null) {
      ValueWrapper value = l1Cache.get(key);
      if (value != null) {
        return value;
      }
    }

    // Try L2 cache
    if (l2Cache != null) {
      ValueWrapper value = l2Cache.get(key);
      if (value != null) {
        // Write back to L1 cache for faster access
        if (l1Cache != null) {
          l1Cache.put(key, value.get());
        }
        return value;
      }
    }

    return null;
  }

  @Override
  public <T> T get(Object key, Class<T> type) {
    // Try L1 cache first
    if (l1Cache != null) {
      T value = l1Cache.get(key, type);
      if (value != null) {
        return value;
      }
    }

    // Try L2 cache
    if (l2Cache != null) {
      T value = l2Cache.get(key, type);
      if (value != null) {
        // Write back to L1 cache for faster access
        if (l1Cache != null) {
          l1Cache.put(key, value);
        }
        return value;
      }
    }

    return null;
  }

  @Override
  public <T> T get(Object key, Callable<T> valueLoader) {
    // Try L1 cache first
    if (l1Cache != null) {
      try {
        return l1Cache.get(key, valueLoader);
      } catch (Exception e) {
        // If L1 fails, try L2
      }
    }

    // Try L2 cache
    if (l2Cache != null) {
      try {
        T value = l2Cache.get(key, valueLoader);
        if (value != null && l1Cache != null) {
          l1Cache.put(key, value);
        }
        return value;
      } catch (Exception e) {
        // If both fail, throw the exception
        throw new RuntimeException("Failed to load value from cache", e);
      }
    }

    throw new RuntimeException("No cache available");
  }

  @Override
  public void put(Object key, Object value) {
    // Write to both caches
    if (l1Cache != null) {
      l1Cache.put(key, value);
    }
    if (l2Cache != null) {
      l2Cache.put(key, value);
    }
  }

  @Override
  public void evict(Object key) {
    // Evict from both caches
    if (l1Cache != null) {
      l1Cache.evict(key);
    }
    if (l2Cache != null) {
      l2Cache.evict(key);
    }
  }

  @Override
  public void clear() {
    // Clear both caches
    if (l1Cache != null) {
      l1Cache.clear();
    }
    if (l2Cache != null) {
      l2Cache.clear();
    }
  }
}
