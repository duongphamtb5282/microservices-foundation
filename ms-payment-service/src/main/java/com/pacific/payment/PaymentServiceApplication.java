package com.pacific.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Payment Service Application - Microservices Demo
 * 
 * This service demonstrates:
 * - Event-driven architecture (consumes OrderCreatedEvent)
 * - CQRS pattern using backend-core components
 * - Kafka event consumption with retry logic
 * - Payment processing simulation
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.pacific.payment",
    "com.pacific.core"  // Scan backend-core components
})
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

