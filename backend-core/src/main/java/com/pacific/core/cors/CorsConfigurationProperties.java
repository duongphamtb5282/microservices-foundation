package com.pacific.core.cors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to enable CorsProperties. This allows services to override CORS configuration
 * in their application.yml files.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfigurationProperties {

  /** Log CORS configuration */
  public void logCorsConfiguration() {
    log.info("🔧 CORS Configuration Properties enabled");
    log.info("✅ CorsProperties can be configured via application.yml");
  }
}
