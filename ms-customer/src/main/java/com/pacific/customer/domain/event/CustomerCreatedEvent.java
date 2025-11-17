package com.pacific.customer.domain.event;

import com.pacific.customer.domain.model.CustomerPreferences;
import com.pacific.customer.domain.model.CustomerProfile;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Domain event fired when a new customer is created */
public record CustomerCreatedEvent(
    String eventId,
    String customerId,
    String email,
    CustomerProfile profile,
    CustomerPreferences preferences,
    Instant occurredOn,
    String source,
    Map<String, Object> metadata)
    implements CustomerDomainEvent {

  @Override
  public String getEventId() {
    return eventId;
  }

  public CustomerCreatedEvent {
    Objects.requireNonNull(eventId, "Event ID cannot be null");
    Objects.requireNonNull(customerId, "Customer ID cannot be null");
    Objects.requireNonNull(email, "Email cannot be null");
    Objects.requireNonNull(profile, "Profile cannot be null");
    Objects.requireNonNull(preferences, "Preferences cannot be null");
    Objects.requireNonNull(occurredOn, "Occurred on cannot be null");
    Objects.requireNonNull(source, "Source cannot be null");
  }

  @Override
  public String getEventType() {
    return "CUSTOMER_CREATED";
  }

  @Override
  public String getCustomerId() {
    return customerId;
  }

  @Override
  public Instant getOccurredOn() {
    return occurredOn;
  }

  @Override
  public String getSource() {
    return source;
  }

  @Override
  public Map<String, Object> getMetadata() {
    return metadata != null ? Map.copyOf(metadata) : Map.of();
  }

  @Override
  public String getCorrelationId() {
    return eventId;
  }

  /** Factory method to create event with metadata */
  public static CustomerCreatedEvent create(
      String eventId,
      String customerId,
      String email,
      CustomerProfile profile,
      CustomerPreferences preferences,
      String source) {

    Map<String, Object> metadata =
        Map.of(
            "email_domain", email.substring(email.indexOf('@') + 1),
            "has_phone", profile.phone() != null,
            "language", preferences.language(),
            "timezone", preferences.timezone());

    return new CustomerCreatedEvent(
        eventId, customerId, email, profile, preferences, Instant.now(), source, metadata);
  }
}
