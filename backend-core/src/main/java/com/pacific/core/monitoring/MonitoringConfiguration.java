package com.pacific.core.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Monitoring configuration module with single responsibility. Only handles common monitoring
 * configuration, no service-specific logic.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MonitoringProperties.class)
@ConditionalOnProperty(name = "monitoring.enabled", havingValue = "true", matchIfMissing = true)
public class MonitoringConfiguration {

  public MonitoringConfiguration() {
    log.info("🔧 Monitoring configuration loaded");
    log.info("✅ Monitoring configuration initialized");
  }
}
