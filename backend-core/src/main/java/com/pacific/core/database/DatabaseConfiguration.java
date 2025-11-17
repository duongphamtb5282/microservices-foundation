package com.pacific.core.database;

import java.util.Optional;
import java.util.Properties;

import javax.sql.DataSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database configuration module with single responsibility. Only handles common database
 * configuration, no service-specific logic.
 */
@Slf4j
@Configuration
@EnableJpaRepositories(basePackages = {"com.pacific"})
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
@EnableTransactionManagement
@EnableConfigurationProperties(DatabaseProperties.class)
@RequiredArgsConstructor
public class DatabaseConfiguration {

  private final DatabaseProperties properties;

  @Value("${spring.jpa.hibernate.ddl-auto:update}")
  private String ddlAuto;

  @Value("${spring.jpa.show-sql:false}")
  private boolean showSql;

  @Value("${spring.jpa.properties.hibernate.dialect:org.hibernate.dialect.PostgreSQLDialect}")
  private String dialect;

  @Value("${spring.jpa.properties.hibernate.format_sql:true}")
  private boolean formatSql;

  @Value("${spring.jpa.properties.hibernate.use_sql_comments:true}")
  private boolean useSqlComments;

  @Value("${spring.jpa.properties.hibernate.jdbc.time_zone:UTC}")
  private String timeZone;

  @Value("${spring.jpa.properties.hibernate.cache.use_second_level_cache:false}")
  private boolean useSecondLevelCache;

  @Value("${spring.jpa.properties.hibernate.cache.use_query_cache:false}")
  private boolean useQueryCache;

  @Value(
      "${spring.jpa.properties.hibernate.cache.region.factory_class:org.hibernate.cache.internal.NoCachingRegionFactory}")
  private String cacheRegionFactory;

  /** Log database configuration */
  public void logDatabaseConfiguration() {
    log.info("🔧 Database Configuration:");
    log.info("   - Min Idle: {}", properties.getDefaultMinIdle());
    log.info("   - Max Pool Size: {}", properties.getDefaultMaxPoolSize());
    log.info("   - Connection Timeout: {}", properties.getDefaultConnectionTimeout());
    log.info("   - Idle Timeout: {}", properties.getDefaultIdleTimeout());
    log.info("   - Max Lifetime: {}", properties.getDefaultMaxLifetime());
    log.info("✅ Database configuration logged");
  }

  /** Entity Manager Factory. Single responsibility: Configure JPA entity manager. */
  @Bean
  @ConditionalOnMissingBean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
    log.info("🔧 Configuring JPA Entity Manager Factory");

    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource);
    em.setPackagesToScan("com.pacific.**.entity");

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    em.setJpaVendorAdapter(vendorAdapter);

    Properties properties = new Properties();
    properties.setProperty("hibernate.hbm2ddl.auto", ddlAuto);
    properties.setProperty("hibernate.show_sql", String.valueOf(showSql));
    properties.setProperty("hibernate.dialect", dialect);
    properties.setProperty("hibernate.format_sql", String.valueOf(formatSql));
    properties.setProperty("hibernate.use_sql_comments", String.valueOf(useSqlComments));
    properties.setProperty("hibernate.jdbc.time_zone", timeZone);

    // Second level cache configuration
    properties.setProperty(
        "hibernate.cache.use_second_level_cache", String.valueOf(useSecondLevelCache));
    properties.setProperty("hibernate.cache.use_query_cache", String.valueOf(useQueryCache));
    properties.setProperty("hibernate.cache.region.factory_class", cacheRegionFactory);

    em.setJpaProperties(properties);

    log.info("✅ JPA Entity Manager Factory configured");
    return em;
  }

  /** Transaction manager configuration. Single responsibility: Configure transaction management. */
  @Bean
  @ConditionalOnMissingBean
  public PlatformTransactionManager transactionManager(
      LocalContainerEntityManagerFactoryBean entityManagerFactory) {
    log.info("🔧 Configuring JPA transaction manager");

    JpaTransactionManager transactionManager = new JpaTransactionManager();
    transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());

    log.info("✅ JPA transaction manager configured");
    return transactionManager;
  }

  /**
   * Spring Security Auditor Aware. Single responsibility: Provide auditor information for JPA
   * auditing.
   */
  @Bean(name = "springSecurityAuditorAware")
  public AuditorAware<String> springSecurityAuditorAware() {
    log.info("🔧 Configuring Spring Security Auditor Aware");
    SpringSecurityAuditorAware auditorAware = new SpringSecurityAuditorAware();
    log.info("✅ Spring Security Auditor Aware bean created: {}", auditorAware);
    return auditorAware;
  }

  /**
   * Spring Security Auditor Aware implementation. Single responsibility: Extract current user for
   * auditing.
   */
  public static class SpringSecurityAuditorAware implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null
          || !authentication.isAuthenticated()
          || "anonymousUser".equals(authentication.getPrincipal())) {
        // For testing and when no authentication context is available,
        // return a default user ID to ensure audit fields are populated
        System.out.println(
            "🔍 JPA Auditing: No authenticated user found, using 'system' as auditor");
        return Optional.of("system");
      }

      // In a real application, you would extract the user ID from the authentication principal
      String auditor = authentication.getName();
      System.out.println("🔍 JPA Auditing: Using authenticated user '" + auditor + "' as auditor");
      return Optional.of(auditor);
    }
  }
}
