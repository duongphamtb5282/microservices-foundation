package com.pacific.core.database;

import com.zaxxer.hikari.HikariConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** HikariCP configuration with single responsibility. Handles HikariCP properties binding only. */
@Slf4j
@Configuration
public class HikariConfiguration {

  /**
   * HikariConfig bean with properties binding. Single responsibility: Provide HikariConfig with
   * properties binding.
   */
  @Bean
  @ConfigurationProperties(prefix = "spring.datasource.hikari")
  public HikariConfig hikariConfig() {
    log.info("🔧 Configuring HikariConfig with properties binding");
    return new HikariConfig();
  }
}
