package com.pacific.core.messaging.cqrs.event.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.pacific.core.messaging.cqrs.event.EventPublisher;
import com.pacific.shared.messaging.cqrs.event.DomainEvent;

/** Kafka-based implementation of EventPublisher. */
@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaEventPublisher implements EventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
    log.info("🚀 KafkaEventPublisher created successfully with KafkaTemplate: {}", kafkaTemplate);
  }

  @Override
  public <T extends DomainEvent> CompletableFuture<SendResult<String, T>> publish(
      String topic, T event) {
    return publish(topic, event.getEventId(), event, new HashMap<>());
  }

  @Override
  public <T extends DomainEvent> CompletableFuture<SendResult<String, T>> publish(
      String topic, String key, T event) {
    return publish(topic, key, event, new HashMap<>());
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends DomainEvent> CompletableFuture<SendResult<String, T>> publish(
      String topic, String key, T event, Map<String, String> headers) {

    log.info("Publishing event {} to topic {} (key: {})", event.getEventType(), topic, key);

    ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, event);

    // Add event headers
    headers.forEach((k, v) -> record.headers().add(k, v.getBytes()));
    record.headers().add("event-type", event.getEventType().getBytes());
    record.headers().add("event-id", event.getEventId().getBytes());

    if (event.getCorrelationId() != null) {
      record.headers().add("correlation-id", event.getCorrelationId().getBytes());
    }

    CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);

    future.whenComplete(
        (result, ex) -> {
          if (ex == null) {
            log.info(
                "Event {} published successfully to partition {} with offset {}",
                event.getEventType(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
          } else {
            log.error("Failed to publish event {} to topic {}", event.getEventType(), topic, ex);
          }
        });

    return (CompletableFuture<SendResult<String, T>>) (CompletableFuture<?>) future;
  }
}
