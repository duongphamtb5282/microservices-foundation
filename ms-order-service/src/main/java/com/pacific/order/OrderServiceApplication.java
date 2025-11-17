package com.pacific.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * Order Service Application - Microservices Demo
 * 
 * This service demonstrates:
 * - Clean Architecture with proper layer separation
 * - CQRS pattern using backend-core components
 * - Event-driven communication via Kafka
 * - Authentication via Feign client
 * - Database migrations with Liquibase
 */
@SpringBootApplication
@EnableFeignClients
@ComponentScan(basePackages = {
    "com.pacific.order",
    "com.pacific.core"  // Scan backend-core components
})
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}

