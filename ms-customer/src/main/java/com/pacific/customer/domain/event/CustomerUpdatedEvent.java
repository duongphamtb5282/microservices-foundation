package com.pacific.customer.domain.event;

import com.pacific.customer.domain.model.CustomerPreferences;
import com.pacific.customer.domain.model.CustomerProfile;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Domain event fired when customer information is updated */
public record CustomerUpdatedEvent(
    String eventId,
    String customerId,
    CustomerProfile oldProfile,
    CustomerProfile newProfile,
    CustomerPreferences oldPreferences,
    CustomerPreferences newPreferences,
    Instant occurredOn,
    String source,
    Map<String, Object> metadata)
    implements CustomerDomainEvent {

  @Override
  public String getEventId() {
    return eventId;
  }

  public CustomerUpdatedEvent {
    Objects.requireNonNull(eventId, "Event ID cannot be null");
    Objects.requireNonNull(customerId, "Customer ID cannot be null");
    Objects.requireNonNull(occurredOn, "Occurred on cannot be null");
    Objects.requireNonNull(source, "Source cannot be null");
  }

  @Override
  public String getEventType() {
    return "CUSTOMER_UPDATED";
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

  /** Checks if profile was changed */
  public boolean profileChanged() {
    return !Objects.equals(oldProfile, newProfile);
  }

  /** Checks if preferences were changed */
  public boolean preferencesChanged() {
    return !Objects.equals(oldPreferences, newPreferences);
  }

  /** Factory method to create update event */
  public static CustomerUpdatedEvent create(
      String eventId,
      String customerId,
      CustomerProfile oldProfile,
      CustomerProfile newProfile,
      CustomerPreferences oldPreferences,
      CustomerPreferences newPreferences,
      String source) {

    Map<String, Object> metadata =
        Map.of(
            "profile_changed",
            !Objects.equals(oldProfile, newProfile),
            "preferences_changed",
            !Objects.equals(oldPreferences, newPreferences),
            "old_language",
            oldPreferences != null ? oldPreferences.language() : null,
            "new_language",
            newPreferences != null ? newPreferences.language() : null);

    return new CustomerUpdatedEvent(
        eventId,
        customerId,
        oldProfile,
        newProfile,
        oldPreferences,
        newPreferences,
        Instant.now(),
        source,
        metadata);
  }
}
