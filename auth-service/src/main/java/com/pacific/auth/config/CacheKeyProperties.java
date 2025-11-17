package com.pacific.auth.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for cache key patterns. Centralizes cache key management to prevent
 * hardcoded values.
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth-service.cache")
public class CacheKeyProperties {

  /** Service prefix for all cache keys to prevent collisions in shared Redis */
  private String servicePrefix;

  /** Cache names */
  private final CacheNames names = new CacheNames();

  /** Cache key patterns for token management */
  private final CacheKeyPatterns patterns = new CacheKeyPatterns();

  /** Cache TTL settings */
  private final CacheTtl ttl = new CacheTtl();

  /** Cache reload settings */
  private boolean reloadOnStartup;

  private boolean scheduledReloadEnabled;

  @PostConstruct
  void validateConfiguration() {
    if (!StringUtils.hasText(servicePrefix)) {
      throw new IllegalStateException(
          "auth-service.cache.service-prefix must be configured (property missing)");
    }
  }

  @Data
  public static class CacheNames {
    private String users;
    private String roles;
    private String tokens;
    private String permissions;
  }

  @Data
  public static class CacheKeyPatterns {
    private String blacklistedToken;
    private String blacklistedTokenHash;
    private String userBlacklisted;
    private String userActiveTokens;
  }

  @Data
  public static class CacheTtl {
    private String user;
    private String role;
    private String token;
    private String permission;
  }

  /** Build a prefixed cache key */
  public String buildKey(String pattern, String identifier) {
    return String.format("%s:%s:%s", servicePrefix, pattern, identifier);
  }

  /** Build a user cache key */
  public String buildUserKey(String userId) {
    return buildKey(patterns.userBlacklisted, userId);
  }

  /** Build a token blacklist key */
  public String buildTokenBlacklistKey(String tokenId) {
    return buildKey(patterns.blacklistedToken, tokenId);
  }

  /** Build a token hash blacklist key */
  public String buildTokenHashBlacklistKey(String tokenHash) {
    return buildKey(patterns.blacklistedTokenHash, tokenHash);
  }

  /** Build a user active tokens key */
  public String buildUserActiveTokensKey(String username) {
    return buildKey(patterns.userActiveTokens, username);
  }
}
