package com.pacific.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** API Gateway Application Central entry point for all microservices */
@SpringBootApplication
// Enables @Scheduled JWKS key refresh in KeycloakJwksService (F-35)
@EnableScheduling
@ComponentScan(
    basePackages = {"com.pacific.gateway", "com.pacific.core", "com.pacific.shared.exceptions"})
public class ApiGatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(ApiGatewayApplication.class, args);
  }
}
