package com.pacific.core.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Base security configuration for backend-core. Provides common security beans that can be reused
 * across all services. Services can override these beans by providing their own implementations.
 *
 * <p>Gated on the spring-security-crypto classpath ({@code @ConditionalOnClass}): this config is
 * imported into every consumer via BackendCoreAutoConfiguration, and consumers without Spring
 * Security at all (e.g. ms-payment) would crash Spring's OnBeanCondition while introspecting the
 * factory method's return type (NoClassDefFoundError: PasswordEncoder). The gate is evaluated from
 * ASM metadata before any class is loaded. Consumers that only have spring-security-core (via
 * spring-cloud-commons) or full security still get the bean.
 */
@Configuration
@Slf4j
@ConditionalOnClass(PasswordEncoder.class)
public class SecurityConfig {

  /**
   * Default password encoder configuration using BCrypt. Services can override this by providing
   * their own PasswordEncoder bean. @ConditionalOnMissingBean ensures this is only created if no
   * other PasswordEncoder exists
   */
  @Bean
  @ConditionalOnMissingBean(PasswordEncoder.class)
  public PasswordEncoder passwordEncoder() {
    log.info("🔐 Configuring default BCrypt password encoder from backend-core");
    return new BCryptPasswordEncoder();
  }
}
