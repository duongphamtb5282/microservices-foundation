package com.pacific.shared.events;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.pacific.shared.messaging.cqrs.event.DomainEvent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain event representing user creation. This event is published by the auth-service when a user
 * registers and consumed by other services like customer-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEvent implements Serializable, DomainEvent {

  private static final long serialVersionUID = 1L;

  // Event payload
  private String userId;
  private String username;
  private String email;

  // Correlation and tracing
  @Builder.Default private String correlationId = UUID.randomUUID().toString();

  // Event metadata
  @Builder.Default private String eventType = "USER_CREATED";

  @Builder.Default private String version = "1.0";

  @Builder.Default private String sourceService = "auth-service";

  @Builder.Default private LocalDateTime timestamp = LocalDateTime.now();

  // Distributed tracing support (optional)
  private String traceId;
  private String spanId;

  // Additional user metadata (optional)
  private String firstName;
  private String lastName;
  private String phone;
  private String role;

  // Constructors for backward compatibility
  public UserCreatedEvent(String userId, String username, String email) {
    this.userId = userId;
    this.username = username;
    this.email = email;
    this.correlationId = UUID.randomUUID().toString();
    this.eventType = "USER_CREATED";
    this.version = "1.0";
    this.sourceService = "auth-service";
    this.timestamp = LocalDateTime.now();
  }

  // Domain event interface methods
  @Override
  public String getEventId() {
    return userId != null ? "USER_CREATED_" + userId : correlationId;
  }

  @Override
  public String getAggregateId() {
    return userId; // User ID is the aggregate ID for user events
  }

  @Override
  public Instant getOccurredOn() {
    return timestamp != null ? timestamp.toInstant(java.time.ZoneOffset.UTC) : Instant.now();
  }

  @Override
  public String getSource() {
    return sourceService;
  }

  @Override
  public Map<String, Object> getMetadata() {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("userId", userId);
    metadata.put("username", username);
    metadata.put("email", email);
    metadata.put("correlationId", correlationId);
    if (firstName != null) metadata.put("firstName", firstName);
    if (lastName != null) metadata.put("lastName", lastName);
    if (role != null) metadata.put("role", role);
    return metadata;
  }

  // Validation methods
  public boolean isValid() {
    return userId != null
        && !userId.trim().isEmpty()
        && username != null
        && !username.trim().isEmpty()
        && email != null
        && !email.trim().isEmpty()
        && correlationId != null
        && !correlationId.trim().isEmpty();
  }

  public void validate() {
    if (!isValid()) {
      throw new IllegalArgumentException("UserCreatedEvent is not valid: " + this);
    }
  }

  // Convenience methods
  public UserCreatedEvent withCorrelationId(String correlationId) {
    this.correlationId =
        correlationId != null && !correlationId.trim().isEmpty()
            ? correlationId.trim()
            : UUID.randomUUID().toString();
    return this;
  }

  public UserCreatedEvent withTraceId(String traceId) {
    this.traceId = traceId;
    return this;
  }

  // Note: Lombok @Builder handles correlationId properly
  // The @Builder.Default annotation ensures a UUID is generated if not provided

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserCreatedEvent that = (UserCreatedEvent) o;
    return Objects.equals(userId, that.userId)
        && Objects.equals(username, that.username)
        && Objects.equals(email, that.email)
        && Objects.equals(correlationId, that.correlationId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, username, email, correlationId);
  }

  @Override
  public String toString() {
    return "UserCreatedEvent{"
        + "userId='"
        + userId
        + '\''
        + ", username='"
        + username
        + '\''
        + ", email='"
        + email
        + '\''
        + ", correlationId='"
        + correlationId
        + '\''
        + ", eventType='"
        + eventType
        + '\''
        + ", sourceService='"
        + sourceService
        + '\''
        + ", timestamp="
        + timestamp
        + '}';
  }
}
