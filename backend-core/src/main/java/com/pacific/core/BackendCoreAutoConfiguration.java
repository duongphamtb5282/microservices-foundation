package com.pacific.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import com.pacific.core.cache.CacheConfig;
import com.pacific.core.database.DatabaseConfiguration;
import com.pacific.core.database.mongo.MongoConfiguration;
import com.pacific.core.monitoring.MonitoringConfiguration;
import com.pacific.core.security.SecurityConfig;

/**
 * Auto-configuration for backend-core. Single responsibility: Auto-configure all backend-core
 * modules.
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(BackendCoreProperties.class)
@ComponentScan("com.pacific.core")
@Import({
  CacheConfig.class,
  DatabaseConfiguration.class,
  MongoConfiguration.class,
  MonitoringConfiguration.class,
  SecurityConfig.class
})
public class BackendCoreAutoConfiguration {

  public BackendCoreAutoConfiguration() {
    log.info("🔧 Backend-core auto-configuration loaded");
    log.info("✅ Available modules: Cache, Database, MongoDB, Monitoring, Security");
  }
}
