package com.pacific.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Payment Service Application - Microservices Demo
 *
 * <p>This service demonstrates: - Event-driven architecture (consumes OrderCreatedEvent) - CQRS
 * pattern using backend-core components - Kafka event consumption with retry logic - Payment
 * processing simulation
 */
@SpringBootApplication
@ComponentScan(
    basePackages = {
      "com.pacific.payment",
      "com.pacific.core" // Scan backend-core components
    })
@EnableScheduling // Payment result outbox relay (F-02 saga return path)
public class PaymentServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(PaymentServiceApplication.class, args);
  }
}
