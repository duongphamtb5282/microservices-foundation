package com.pacific.auth.config.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for rate limiting. Externalizes rate limiting configuration to prevent
 * hardcoded values.
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "auth-service.security.rate-limiting")
public class RateLimitingProperties {

  /** Enable or disable rate limiting globally */
  private boolean enabled = true;

  /** Maximum requests per minute for general endpoints */
  @Min(value = 1, message = "Max requests per minute must be at least 1")
  private int maxRequestsPerMinute = 60;

  /** Maximum login attempts per minute per IP */
  @Min(value = 1, message = "Max login attempts per minute must be at least 1")
  private int maxLoginAttemptsPerMinute = 5;

  /** Window size in milliseconds for rate limiting */
  @Min(value = 1000, message = "Window size must be at least 1000ms")
  private long windowSizeMillis = 60000; // 1 minute

  /** Use Redis for distributed rate limiting (recommended for multi-instance deployments) */
  private boolean useRedis = false;

  /** Endpoint-specific rate limits */
  @NotNull private Map<String, EndpointRateLimit> endpoints = new HashMap<>();

  /** Rate limit configuration for a specific endpoint */
  @Data
  public static class EndpointRateLimit {
    @Min(value = 1, message = "Max requests must be at least 1")
    private int maxRequests;

    @Min(value = 1000, message = "Window must be at least 1000ms")
    private long windowMillis;
  }
}
