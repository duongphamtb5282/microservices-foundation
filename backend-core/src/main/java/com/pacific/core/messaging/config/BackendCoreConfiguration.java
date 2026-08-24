package com.pacific.core.messaging.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pacific.core.messaging.consumer.BaseEventConsumer;

/** Main configuration class for Backend Core messaging components. */
@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class BackendCoreConfiguration {

  // NOTE: CircuitBreakerService is a @Service component (single definition point) — the manual
  // @Bean was removed to avoid a duplicate-bean conflict when kafka.enabled=true (auth).
  @Bean
  public BaseEventConsumer.EventProcessingStats eventProcessingStats() {
    // This would be injected into BaseEventConsumer instances
    return new BaseEventConsumer.EventProcessingStats(0, 0);
  }
}
