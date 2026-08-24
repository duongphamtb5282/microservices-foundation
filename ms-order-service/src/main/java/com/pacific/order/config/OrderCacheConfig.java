package com.pacific.order.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Cache configuration for Order Service. Implements multi-level caching: Redis (distributed) +
 * Caffeine (local).
 *
 * <p>Named {@code OrderCacheConfig} (not {@code CacheConfig}) to avoid a bean-name clash with
 * {@code com.pacific.core.cache.CacheConfig}, which BackendCoreAutoConfiguration's
 * {@code @ComponentScan("com.pacific.core")} pulls into this context — two same-named
 * {@code @Configuration} classes fail startup with a ConflictingBeanDefinitionException. Core's
 * cache stack is disabled for this service via {@code cache.enabled: false} (application.yml), so
 * this config is the sole cache provider here.
 */
@Configuration
@EnableCaching
public class OrderCacheConfig {

  @Bean
  public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
    org.springframework.data.redis.cache.RedisCacheConfiguration config =
        org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30)) // Default TTL for all caches
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(config)
        .withCacheConfiguration("orders", config.entryTtl(Duration.ofMinutes(15)))
        .withCacheConfiguration("order-details", config.entryTtl(Duration.ofMinutes(30)))
        .withCacheConfiguration("user-orders", config.entryTtl(Duration.ofMinutes(10)))
        .build();
  }

  @Bean
  public CacheManager localCacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.setCaffeine(
        Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats());

    return cacheManager;
  }

  @Bean
  // Primary: three CacheManager beans coexist here (redis, local, composite); the @Cacheable
  // handlers (GetOrderById/GetOrdersByUser/GetUserOrdersPage) and the eviction sites
  // (CancelOrder/CreateOrder/PaymentResult) resolve to this composite facade, so reads, writes and
  // evictions all hit both tiers. Without @Primary, cache-annotated methods would throw
  // NoUniqueBeanDefinitionException at runtime.
  @Primary
  public org.springframework.cache.CacheManager multiLevelCacheManager(
      // Both params are CacheManager-typed; qualifiers pin each to its bean name, otherwise
      // injection is ambiguous once core's cache stack is out of the context.
      @Qualifier("redisCacheManager") CacheManager redisCacheManager,
      @Qualifier("localCacheManager") CacheManager localCacheManager) {

    return new org.springframework.cache.support.CompositeCacheManager(
        redisCacheManager, localCacheManager);
  }
}
