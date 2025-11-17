package com.pacific.auth;

import com.pacific.core.BackendCoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * Auth Service Application Handles user management, authentication, and authorization Now uses
 * backend-core auto-configuration for common functionality
 */
@SpringBootApplication(
    exclude = {
      org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration
          .class
    })
@Import(BackendCoreAutoConfiguration.class)
@EnableFeignClients(basePackages = "com.pacific.auth.modules.authentication.client")
@ComponentScan(
    basePackages = {
      "com.pacific.auth" // Auth-service specific packages only
    })
public class AuthServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuthServiceApplication.class, args);
  }
}
