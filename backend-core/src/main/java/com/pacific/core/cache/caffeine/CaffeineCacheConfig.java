package com.pacific.core.cache.caffeine;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pacific.core.BackendCoreProperties;

/**
 * Caffeine cache configuration for L1 cache. Uses externalized properties for TTL and size
 * configuration.
 */
@Configuration
@ConditionalOnProperty(
    name = "cache.caffeine.enabled",
    havingValue = "true",
    matchIfMissing = false)
@Slf4j
@RequiredArgsConstructor
public class CaffeineCacheConfig {

  private final BackendCoreProperties backendCoreProperties;

  /** Caffeine cache manager for L1 cache with configurable settings. */
  @Bean
  public CacheManager caffeineCacheManager() {
    BackendCoreProperties.Cache.L1Cache l1Cache = backendCoreProperties.getCache().getL1Cache();

    log.info(
        "Configuring Caffeine cache manager for L1 cache - maxSize: {}, TTL: {} minutes",
        l1Cache.getMaxSize(),
        l1Cache.getTtlMinutes());

    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.setCaffeine(
        Caffeine.newBuilder()
            .maximumSize(l1Cache.getMaxSize())
            .expireAfterWrite(Duration.ofMinutes(l1Cache.getTtlMinutes()))
            .recordStats());

    return cacheManager;
  }
}
