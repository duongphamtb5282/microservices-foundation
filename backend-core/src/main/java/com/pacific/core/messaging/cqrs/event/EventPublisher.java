package com.pacific.core.messaging.cqrs.event;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.support.SendResult;

import com.pacific.shared.messaging.cqrs.event.DomainEvent;

/** Publisher for domain events. Publishes events to Kafka topics for event-driven architecture. */
public interface EventPublisher {

  /**
   * Publish event to a topic.
   *
   * @param topic The Kafka topic
   * @param event The event to publish
   * @param <T> Event type
   * @return CompletableFuture with send result
   */
  <T extends DomainEvent> CompletableFuture<SendResult<String, T>> publish(String topic, T event);

  /**
   * Publish event with custom key for partitioning.
   *
   * @param topic The Kafka topic
   * @param key The partition key
   * @param event The event to publish
   * @param <T> Event type
   * @return CompletableFuture with send result
   */
  <T extends DomainEvent> CompletableFuture<SendResult<String, T>> publish(
      String topic, String key, T event);

  /**
   * Publish event with custom headers.
   *
   * @param topic The Kafka topic
   * @param key The partition key
   * @param event The event to publish
   * @param headers Custom headers
   * @param <T> Event type
   * @return CompletableFuture with send result
   */
  <T extends DomainEvent> CompletableFuture<SendResult<String, T>> publish(
      String topic, String key, T event, Map<String, String> headers);
}
