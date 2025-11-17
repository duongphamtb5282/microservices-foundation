package com.pacific.core.monitoring;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Monitoring configuration properties for backend-core. Allows services to configure monitoring
 * settings without "backend-core" prefix.
 */
@Data
@ConfigurationProperties(prefix = "monitoring")
public class MonitoringProperties {

  private boolean enabled = true;
  private boolean enableHealthChecks = true;
  private boolean enableMetrics = true;
  private boolean enableLogging = true;
  private String logLevel = "INFO";
}
