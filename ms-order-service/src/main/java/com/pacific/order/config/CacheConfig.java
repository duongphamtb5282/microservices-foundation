package com.pacific.order.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Cache configuration for Order Service.
 * Implements multi-level caching: Redis (distributed) + Caffeine (local).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        org.springframework.data.redis.cache.RedisCacheConfiguration config =
            org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // Default TTL for all caches
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer()))
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
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats());

        return cacheManager;
    }

    @Bean
    public org.springframework.cache.CacheManager multiLevelCacheManager(
            CacheManager redisCacheManager,
            CacheManager localCacheManager) {

        return new org.springframework.cache.support.CompositeCacheManager(
            redisCacheManager, localCacheManager);
    }
}
