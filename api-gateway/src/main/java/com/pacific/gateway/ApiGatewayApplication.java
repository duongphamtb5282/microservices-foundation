package com.pacific.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * API Gateway Application
 * Central entry point for all microservices
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.pacific.gateway", "com.pacific.core", "com.pacific.shared.exceptions"})
public class ApiGatewayApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
