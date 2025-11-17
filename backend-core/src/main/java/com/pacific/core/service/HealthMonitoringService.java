package com.pacific.core.service;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/** Centralized health monitoring service for all microservices */
@Slf4j
@Service
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true", matchIfMissing = true)
public class HealthMonitoringService implements HealthIndicator {

  private final DataSource dataSource;
  private final RedisTemplate<String, Object> redisTemplate;

  @Autowired
  public HealthMonitoringService(
      DataSource dataSource, RedisTemplate<String, Object> redisTemplate) {
    this.dataSource = dataSource;
    this.redisTemplate = redisTemplate;
  }

  @Override
  public Health health() {
    try {
      Map<String, Object> details = new HashMap<>();

      // Check database health
      boolean dbHealthy = checkDatabaseHealth();
      details.put("database", dbHealthy ? "UP" : "DOWN");

      // Check Redis health
      boolean redisHealthy = checkRedisHealth();
      details.put("redis", redisHealthy ? "UP" : "DOWN");

      // Overall health
      boolean overallHealthy = dbHealthy && redisHealthy;
      String status = overallHealthy ? "UP" : "DOWN";

      details.put("timestamp", LocalDateTime.now());
      details.put("service", "backend-core");

      if (overallHealthy) {
        return Health.up().withDetails(details).build();
      } else {
        return Health.down().withDetails(details).build();
      }

    } catch (Exception e) {
      log.error("Health check failed", e);
      return Health.down()
          .withDetail("error", e.getMessage())
          .withDetail("timestamp", LocalDateTime.now())
          .build();
    }
  }

  /** Check database connectivity */
  private boolean checkDatabaseHealth() {
    try (Connection connection = dataSource.getConnection()) {
      return connection.isValid(5); // 5 second timeout
    } catch (Exception e) {
      log.error("Database health check failed", e);
      return false;
    }
  }

  /** Check Redis connectivity */
  private boolean checkRedisHealth() {
    try {
      redisTemplate.opsForValue().get("health-check");
      return true;
    } catch (Exception e) {
      log.error("Redis health check failed", e);
      return false;
    }
  }

  /** Get detailed system health information */
  public SystemHealth getSystemHealth() {
    boolean dbHealthy = checkDatabaseHealth();
    boolean redisHealthy = checkRedisHealth();

    SystemHealth.HealthStatus overallStatus =
        (dbHealthy && redisHealthy) ? SystemHealth.HealthStatus.UP : SystemHealth.HealthStatus.DOWN;

    SystemHealth health =
        SystemHealth.builder()
            .status(overallStatus)
            .timestamp(LocalDateTime.now())
            .database(dbHealthy)
            .redis(redisHealthy)
            .service("backend-core")
            .build();

    // Event publishing removed to maintain backend-core independence

    return health;
  }

  /** System health DTO */
  @lombok.Data
  @lombok.Builder
  public static class SystemHealth {
    private HealthStatus status;
    private LocalDateTime timestamp;
    private boolean database;
    private boolean redis;
    private String service;

    public enum HealthStatus {
      UP,
      DOWN,
      DEGRADED
    }
  }
}
