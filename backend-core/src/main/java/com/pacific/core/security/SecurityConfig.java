package com.pacific.core.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Base security configuration for backend-core. Provides common security beans that can be reused
 * across all services. Services can override these beans by providing their own implementations.
 */
@Configuration
@Slf4j
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
