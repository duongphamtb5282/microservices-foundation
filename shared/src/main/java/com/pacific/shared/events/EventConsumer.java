package com.pacific.shared.events;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import lombok.extern.slf4j.Slf4j;

/** Generic event consumer configuration for handling events from Kafka */
@Slf4j
@Configuration
public class EventConsumer {

  /** Generic event handler that logs all received events */
  @Bean
  public Consumer<Message<BaseEvent>> eventHandler() {
    return message -> {
      BaseEvent event = message.getPayload();
      log.info(
          "Received event: {} with ID: {} from source: {}",
          event.getEventType(),
          event.getEventId(),
          event.getSource());

      // Add any common event processing logic here
      processEvent(event);
    };
  }

  /** Process the received event */
  private void processEvent(BaseEvent event) {
    try {
      switch (event.getEventType()) {
        case "UserCreated":
          handleUserCreated((UserEvent.UserCreated) event);
          break;
        case "UserUpdated":
          handleUserUpdated((UserEvent.UserUpdated) event);
          break;
        case "UserDeleted":
          handleUserDeleted((UserEvent.UserDeleted) event);
          break;
        case "CommentCreated":
          handleCommentCreated((CommentEvent.CommentCreated) event);
          break;
        case "CommentUpdated":
          handleCommentUpdated((CommentEvent.CommentUpdated) event);
          break;
        case "CommentDeleted":
          handleCommentDeleted((CommentEvent.CommentDeleted) event);
          break;
        case "CacheCleared":
          handleCacheCleared((FoundationEvent.CacheCleared) event);
          break;
        case "SystemHealthChanged":
          handleSystemHealthChanged((FoundationEvent.SystemHealthChanged) event);
          break;
        default:
          log.warn("Unknown event type: {}", event.getEventType());
      }
    } catch (Exception e) {
      log.error("Error processing event {}: {}", event.getEventType(), e.getMessage(), e);
    }
  }

  // Event handlers - can be overridden by specific services
  protected void handleUserCreated(UserEvent.UserCreated event) {
    log.info("Handling UserCreated event for user: {}", event.getUsername());
  }

  protected void handleUserUpdated(UserEvent.UserUpdated event) {
    log.info("Handling UserUpdated event for user: {}", event.getUsername());
  }

  protected void handleUserDeleted(UserEvent.UserDeleted event) {
    log.info("Handling UserDeleted event for user: {}", event.getUsername());
  }

  protected void handleCommentCreated(CommentEvent.CommentCreated event) {
    log.info("Handling CommentCreated event for comment: {}", event.getCommentId());
  }

  protected void handleCommentUpdated(CommentEvent.CommentUpdated event) {
    log.info("Handling CommentUpdated event for comment: {}", event.getCommentId());
  }

  protected void handleCommentDeleted(CommentEvent.CommentDeleted event) {
    log.info("Handling CommentDeleted event for comment: {}", event.getCommentId());
  }

  protected void handleCacheCleared(FoundationEvent.CacheCleared event) {
    log.info("Handling CacheCleared event for cache: {}", event.getCacheName());
  }

  protected void handleSystemHealthChanged(FoundationEvent.SystemHealthChanged event) {
    log.info("Handling SystemHealthChanged event for component: {}", event.getComponent());
  }
}
