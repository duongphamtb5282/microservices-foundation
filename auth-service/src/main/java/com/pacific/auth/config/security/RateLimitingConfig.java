package com.pacific.auth.config.security;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limiting configuration to prevent brute force attacks. Uses configurable properties instead
 * of hardcoded values. For production multi-instance deployments, consider using Redis-based rate
 * limiting.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "auth-service.security.rate-limiting.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class RateLimitingConfig {

  private final RateLimitingProperties rateLimitingProperties;
  private final SecurityEndpointsProperties securityEndpointsProperties;

  private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AtomicInteger> loginAttempts = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Long> keyTimestamps = new ConcurrentHashMap<>();

  private ScheduledExecutorService cleanupScheduler;

  @PostConstruct
  public void init() {
    // Schedule periodic cleanup of expired entries
    cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
    cleanupScheduler.scheduleAtFixedRate(
        this::cleanupExpiredEntries,
        rateLimitingProperties.getWindowSizeMillis(),
        rateLimitingProperties.getWindowSizeMillis(),
        TimeUnit.MILLISECONDS);
    log.info(
        "Rate limiting initialized with maxRequests={}, maxLoginAttempts={}, windowSize={}ms",
        rateLimitingProperties.getMaxRequestsPerMinute(),
        rateLimitingProperties.getMaxLoginAttemptsPerMinute(),
        rateLimitingProperties.getWindowSizeMillis());
  }

  @PreDestroy
  public void shutdown() {
    if (cleanupScheduler != null) {
      cleanupScheduler.shutdown();
      try {
        if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
          cleanupScheduler.shutdownNow();
        }
      } catch (InterruptedException e) {
        cleanupScheduler.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  @Bean
  public OncePerRequestFilter rateLimitingFilter() {
    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(
          HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {

        String clientIp = getClientIpAddress(request);
        String requestPath = request.getRequestURI();

        // Apply stricter rate limiting for authentication endpoints
        if (isAuthenticationEndpoint(requestPath)) {
          if (isRateLimited(
              loginAttempts, clientIp, rateLimitingProperties.getMaxLoginAttemptsPerMinute())) {
            log.warn(
                "Rate limit exceeded for login attempts from IP: {} (max: {})",
                clientIp,
                rateLimitingProperties.getMaxLoginAttemptsPerMinute());
            response.setStatus(429); // Too Many Requests
            response
                .getWriter()
                .write("{\"error\":\"Too many login attempts. Please try again later.\"}");
            response.setContentType("application/json");
            return;
          }
        } else {
          // General rate limiting for other endpoints
          if (isRateLimited(
              requestCounts, clientIp, rateLimitingProperties.getMaxRequestsPerMinute())) {
            log.warn(
                "Rate limit exceeded for requests from IP: {} (max: {})",
                clientIp,
                rateLimitingProperties.getMaxRequestsPerMinute());
            response.setStatus(429); // Too Many Requests
            response
                .getWriter()
                .write("{\"error\":\"Too many requests. Please try again later.\"}");
            response.setContentType("application/json");
            return;
          }
        }

        filterChain.doFilter(request, response);
      }
    };
  }

  private boolean isAuthenticationEndpoint(String path) {
    return securityEndpointsProperties.isAuthenticationEndpoint(path);
  }

  private boolean isRateLimited(
      ConcurrentHashMap<String, AtomicInteger> counterMap, String key, int maxRequests) {
    long currentTime = System.currentTimeMillis();
    Long lastResetTime = keyTimestamps.get(key);

    // Check if window has expired
    if (lastResetTime == null
        || (currentTime - lastResetTime) > rateLimitingProperties.getWindowSizeMillis()) {
      counterMap.put(key, new AtomicInteger(0));
      keyTimestamps.put(key, currentTime);
    }

    AtomicInteger counter = counterMap.computeIfAbsent(key, k -> new AtomicInteger(0));
    int currentCount = counter.incrementAndGet();

    return currentCount > maxRequests;
  }

  /** Cleanup expired entries from the rate limiting maps */
  private void cleanupExpiredEntries() {
    long currentTime = System.currentTimeMillis();
    long windowSize = rateLimitingProperties.getWindowSizeMillis();

    keyTimestamps.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > windowSize * 2);

    // Remove corresponding counter entries
    keyTimestamps
        .keySet()
        .forEach(
            key -> {
              if (!keyTimestamps.containsKey(key)) {
                requestCounts.remove(key);
                loginAttempts.remove(key);
              }
            });

    log.debug("Rate limiting cleanup completed. Active keys: {}", keyTimestamps.size());
  }

  private String getClientIpAddress(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }

    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }

    return request.getRemoteAddr();
  }
}
