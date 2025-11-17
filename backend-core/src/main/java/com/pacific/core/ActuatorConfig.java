package com.pacific.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/** Shared actuator configuration for all microservices */
@Slf4j
@Configuration
public class ActuatorConfig {

  @Value("${management.endpoints.web.base-path:/actuator}")
  private String basePath;

  @Value("${management.endpoint.health.show-details:when-authorized}")
  private String healthShowDetails;

  @Value("${management.endpoint.info.enabled:true}")
  private boolean infoEnabled;

  @Value("${management.endpoint.health.enabled:true}")
  private boolean healthEnabled;

  @Value("${management.endpoint.metrics.enabled:true}")
  private boolean metricsEnabled;

  /** Log actuator configuration */
  public void logActuatorConfiguration() {
    log.info("🔧 Actuator Configuration:");
    log.info("   - Base Path: {}", basePath);
    log.info("   - Health Show Details: {}", healthShowDetails);
    log.info("   - Info Enabled: {}", infoEnabled);
    log.info("   - Health Enabled: {}", healthEnabled);
    log.info("   - Metrics Enabled: {}", metricsEnabled);
    log.info("✅ Actuator configuration logged");
  }
}
