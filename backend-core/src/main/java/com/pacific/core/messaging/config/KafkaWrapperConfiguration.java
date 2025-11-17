package com.pacific.core.messaging.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/** Main configuration class for Kafka Wrapper. Enables Kafka and scans for components. */
@Slf4j
@Configuration
@EnableKafka
@EnableConfigurationProperties(KafkaWrapperProperties.class)
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.pacific.core.messaging")
@RequiredArgsConstructor
public class KafkaWrapperConfiguration {

  private final KafkaWrapperProperties properties;

  @PostConstruct
  public void init() {
    log.info("========================================");
    log.info("Kafka Wrapper Configuration");
    log.info("========================================");
    log.info("Enabled: {}", properties.isEnabled());
    log.info("CQRS Enabled: {}", properties.getCqrs().isEnabled());
    log.info("Command Topic: {}", properties.getCqrs().getCommandTopic());
    log.info("Event Topic: {}", properties.getCqrs().getEventTopic());
    log.info("Max Retry Attempts: {}", properties.getRetry().getMaxAttempts());
    log.info("Initial Backoff: {}", properties.getRetry().getInitialBackoff());
    log.info("DLQ Enabled: {}", properties.getRetry().isEnableDlq());
    log.info("DLQ Suffix: {}", properties.getRetry().getDlqTopicSuffix());
    log.info("Monitoring Enabled: {}", properties.getMonitoring().isEnabled());
    log.info("========================================");
  }
}
