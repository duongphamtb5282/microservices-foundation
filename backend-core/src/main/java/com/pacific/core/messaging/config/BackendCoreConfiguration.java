package com.pacific.core.messaging.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pacific.core.messaging.circuitbreaker.CircuitBreakerService;
import com.pacific.core.messaging.consumer.BaseEventConsumer;

/** Main configuration class for Backend Core messaging components. */
@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class BackendCoreConfiguration {

  @Bean
  @ConditionalOnClass(CircuitBreakerRegistry.class)
  public CircuitBreakerService circuitBreakerService(
      CircuitBreakerRegistry circuitBreakerRegistry) {
    return new CircuitBreakerService(circuitBreakerRegistry);
  }

  @Bean
  public BaseEventConsumer.EventProcessingStats eventProcessingStats() {
    // This would be injected into BaseEventConsumer instances
    return new BaseEventConsumer.EventProcessingStats(0, 0);
  }
}
