package com.pacific.auth.config;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom Liquibase configuration for auth-service Replaces the auto-configuration that was excluded
 */
@Configuration
public class LiquibaseConfig {

  @Bean
  @ConditionalOnProperty(name = "liquibase.enabled", havingValue = "true", matchIfMissing = true)
  public SpringLiquibase liquibase(DataSource dataSource) {
    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setChangeLog("classpath:/db/changelog-master.xml");
    liquibase.setDefaultSchema("public");
    liquibase.setShouldRun(true);
    return liquibase;
  }
}
