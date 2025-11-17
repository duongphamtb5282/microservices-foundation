package com.pacific.shared.events;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/** Generic event publisher for publishing events to Kafka */
@Slf4j
@Component
public class EventPublisher {

  private final StreamBridge streamBridge;

  public EventPublisher(StreamBridge streamBridge) {
    this.streamBridge = streamBridge;
  }

  /** Publish an event to a specific topic */
  public void publishEvent(String topic, BaseEvent event) {
    try {
      log.debug("Publishing event {} to topic {}", event.getEventType(), topic);
      streamBridge.send(topic, event);
      log.info(
          "Successfully published event {} with ID {} to topic {}",
          event.getEventType(),
          event.getEventId(),
          topic);
    } catch (Exception e) {
      log.error(
          "Failed to publish event {} to topic {}: {}",
          event.getEventType(),
          topic,
          e.getMessage(),
          e);
      throw new RuntimeException("Failed to publish event", e);
    }
  }

  /** Publish user events */
  public void publishUserEvent(String topic, UserEvent.UserCreated event) {
    publishEvent(topic, event);
  }

  public void publishUserEvent(String topic, UserEvent.UserUpdated event) {
    publishEvent(topic, event);
  }

  public void publishUserEvent(String topic, UserEvent.UserDeleted event) {
    publishEvent(topic, event);
  }

  /** Publish comment events */
  public void publishCommentEvent(String topic, CommentEvent.CommentCreated event) {
    publishEvent(topic, event);
  }

  public void publishCommentEvent(String topic, CommentEvent.CommentUpdated event) {
    publishEvent(topic, event);
  }

  public void publishCommentEvent(String topic, CommentEvent.CommentDeleted event) {
    publishEvent(topic, event);
  }

  /** Publish foundation events */
  public void publishFoundationEvent(String topic, FoundationEvent.CacheCleared event) {
    publishEvent(topic, event);
  }

  public void publishFoundationEvent(String topic, FoundationEvent.SystemHealthChanged event) {
    publishEvent(topic, event);
  }
}
