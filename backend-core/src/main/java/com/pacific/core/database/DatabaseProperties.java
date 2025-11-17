package com.pacific.core.database;

import java.time.Duration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Database configuration properties for backend-core. Allows services to configure database
 * settings without "backend-core" prefix.
 */
@Data
@ConfigurationProperties(prefix = "database")
public class DatabaseProperties {

  private boolean enabled = true;
  private String defaultPoolType = "hikari";
  private int defaultMinIdle = 2;
  private int defaultMaxPoolSize = 10;
  private Duration defaultConnectionTimeout = Duration.ofSeconds(20);
  private Duration defaultIdleTimeout = Duration.ofMinutes(5);
  private Duration defaultMaxLifetime = Duration.ofMinutes(10);

  // Getter methods
  public boolean isEnabled() {
    return enabled;
  }

  public String getDefaultPoolType() {
    return defaultPoolType;
  }

  public int getDefaultMinIdle() {
    return defaultMinIdle;
  }

  public int getDefaultMaxPoolSize() {
    return defaultMaxPoolSize;
  }

  public Duration getDefaultConnectionTimeout() {
    return defaultConnectionTimeout;
  }

  public Duration getDefaultIdleTimeout() {
    return defaultIdleTimeout;
  }

  public Duration getDefaultMaxLifetime() {
    return defaultMaxLifetime;
  }
}
