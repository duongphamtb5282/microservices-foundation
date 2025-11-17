package com.pacific.core.messaging.examples.events;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.Data;

import com.pacific.shared.messaging.cqrs.event.DomainEvent;

/** Example domain event representing user creation. This demonstrates how to extend DomainEvent. */
@Data
public class UserCreatedEvent implements DomainEvent {

  private String userId;
  private String username;
  private String email;

  public UserCreatedEvent() {
    // Default constructor
  }

  public UserCreatedEvent(String userId, String username, String email) {
    this.userId = userId;
    this.username = username;
    this.email = email;
  }

  @Override
  public String getEventId() {
    return userId != null ? "USER_CREATED_" + userId : UUID.randomUUID().toString();
  }

  @Override
  public String getEventType() {
    return "USER_CREATED";
  }

  @Override
  public Instant getOccurredOn() {
    return Instant.now();
  }

  @Override
  public String getSource() {
    // Source is determined by the service that publishes this event
    // This should be overridden or configured by the consuming service
    return "unknown-service";
  }

  @Override
  public String getCorrelationId() {
    return null; // Not implemented for this example
  }

  @Override
  public Map<String, Object> getMetadata() {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("userId", userId);
    metadata.put("username", username);
    return metadata;
  }

  @Override
  public String getAggregateId() {
    return userId; // User ID is the aggregate ID for user events
  }
}
