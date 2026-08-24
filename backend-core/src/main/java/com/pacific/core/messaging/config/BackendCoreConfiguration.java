package com.pacific.core.messaging.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pacific.core.messaging.consumer.BaseEventConsumer;

/** Main configuration class for Backend Core messaging components. */
@Configuration
public class BackendCoreConfiguration {

  @Bean
  public BaseEventConsumer.EventProcessingStats eventProcessingStats() {
    // This would be injected into BaseEventConsumer instances
    return new BaseEventConsumer.EventProcessingStats(0, 0);
  }
}
