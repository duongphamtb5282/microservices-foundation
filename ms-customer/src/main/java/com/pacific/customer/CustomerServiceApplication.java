package com.pacific.customer;

import com.pacific.core.BackendCoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

/**
 * Customer Service Application
 *
 * <p>A reactive microservice for customer management using: - Spring WebFlux for reactive web
 * programming - MongoDB for document storage - WebSocket for real-time communication - CQRS pattern
 * with reactive command/query buses - Event sourcing with Kafka integration
 */
@SpringBootApplication
@EnableReactiveMongoRepositories(
    basePackages = "com.pacific.customer.infrastructure.persistence.repository")
@EnableReactiveMongoAuditing
@Import(BackendCoreAutoConfiguration.class)
public class CustomerServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(CustomerServiceApplication.class, args);
  }
}
