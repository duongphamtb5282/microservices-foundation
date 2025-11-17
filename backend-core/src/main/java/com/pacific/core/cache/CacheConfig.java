package com.pacific.core.cache;

import java.time.Duration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.pacific.core.BackendCoreProperties;
import com.pacific.core.cache.caffeine.CaffeineCacheConfig;
import com.pacific.core.cache.multitier.MultiTierCacheManager;

/**
 * Main cache configuration that sets up both Caffeine (L1) and Redis (L2) caches. Uses externalized
 * properties for TTL configuration.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class CacheConfig {

  private final RedisConnectionFactory redisConnectionFactory;
  private final BackendCoreProperties backendCoreProperties;

  @Autowired(required = false)
  private CaffeineCacheConfig caffeineCacheConfig;

  // Note: HibernateCacheConfig is not used here as Hibernate cache configuration
  // is handled via application.yml properties (spring.jpa.properties.hibernate.cache.*)

  /** Primary cache manager using multi-tier approach (Caffeine L1 + Redis L2) or Redis-only. */
  @Bean
  @Primary
  public CacheManager cacheManager() {
    if (caffeineCacheConfig != null) {
      log.info("Configuring multi-tier cache manager (Caffeine L1 + Redis L2)");
      MultiTierCacheManager multiTierCacheManager = new MultiTierCacheManager();
      multiTierCacheManager.setCaffeineCacheManager(caffeineCacheConfig.caffeineCacheManager());
      multiTierCacheManager.setRedisCacheManager(redisCacheManager());
      return multiTierCacheManager;
    } else {
      log.info("Configuring Redis-only cache manager (Caffeine not available)");
      return redisCacheManager();
    }
  }

  /** Redis cache manager for L2 cache with configurable TTL. */
  @Bean
  public RedisCacheManager redisCacheManager() {
    BackendCoreProperties.Cache.L2Cache l2Cache = backendCoreProperties.getCache().getL2Cache();

    log.info(
        "Configuring Redis cache manager for L2 cache - TTL: {} minutes", l2Cache.getTtlMinutes());

    RedisCacheConfiguration config =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(l2Cache.getTtlMinutes()))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer<>(
                        new com.fasterxml.jackson.databind.ObjectMapper()
                            .registerModule(
                                new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()),
                        Object.class)));

    return RedisCacheManager.builder(redisConnectionFactory).cacheDefaults(config).build();
  }

  /** RedisTemplate bean for direct Redis operations. */
  @Bean
  @ConditionalOnProperty(name = "cache.enabled", havingValue = "true", matchIfMissing = true)
  public RedisTemplate<String, Object> redisTemplate() {
    log.info("Configuring RedisTemplate for direct Redis operations");

    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(redisConnectionFactory);

    // Use String serializer for keys
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());

    // Use JSON serializer for values with JSR310 support
    com.fasterxml.jackson.databind.ObjectMapper objectMapper =
        new com.fasterxml.jackson.databind.ObjectMapper();
    objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    template.setValueSerializer(
        new org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer<>(
            objectMapper, Object.class));
    template.setHashValueSerializer(
        new org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer<>(
            objectMapper, Object.class));

    template.afterPropertiesSet();
    return template;
  }
}
