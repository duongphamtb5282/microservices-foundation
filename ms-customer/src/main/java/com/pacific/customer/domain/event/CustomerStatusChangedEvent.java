package com.pacific.customer.domain.event;

import com.pacific.customer.domain.model.CustomerStatus;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Domain event fired when customer status changes */
public record CustomerStatusChangedEvent(
    String eventId,
    String customerId,
    CustomerStatus oldStatus,
    CustomerStatus newStatus,
    String reason,
    String changedBy,
    Instant occurredOn,
    String source,
    Map<String, Object> metadata)
    implements CustomerDomainEvent {

  @Override
  public String getEventId() {
    return eventId;
  }

  public CustomerStatusChangedEvent {
    Objects.requireNonNull(eventId, "Event ID cannot be null");
    Objects.requireNonNull(customerId, "Customer ID cannot be null");
    Objects.requireNonNull(oldStatus, "Old status cannot be null");
    Objects.requireNonNull(newStatus, "New status cannot be null");
    Objects.requireNonNull(occurredOn, "Occurred on cannot be null");
    Objects.requireNonNull(source, "Source cannot be null");

    if (oldStatus == newStatus) {
      throw new IllegalArgumentException("Old and new status cannot be the same");
    }
  }

  @Override
  public String getEventType() {
    return "CUSTOMER_STATUS_CHANGED";
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

  /** Checks if this is an activation */
  public boolean isActivation() {
    return newStatus == CustomerStatus.ACTIVE;
  }

  /** Checks if this is a deactivation */
  public boolean isDeactivation() {
    return oldStatus == CustomerStatus.ACTIVE && newStatus != CustomerStatus.ACTIVE;
  }

  /** Checks if this is a suspension */
  public boolean isSuspension() {
    return newStatus == CustomerStatus.SUSPENDED;
  }

  /** Factory method to create status change event */
  public static CustomerStatusChangedEvent create(
      String eventId,
      String customerId,
      CustomerStatus oldStatus,
      CustomerStatus newStatus,
      String reason,
      String changedBy,
      String source) {

    Map<String, Object> metadata =
        Map.of(
            "status_transition",
            oldStatus + " -> " + newStatus,
            "is_activation",
            newStatus == CustomerStatus.ACTIVE,
            "is_deactivation",
            oldStatus == CustomerStatus.ACTIVE && newStatus != CustomerStatus.ACTIVE,
            "is_suspension",
            newStatus == CustomerStatus.SUSPENDED,
            "reason_provided",
            reason != null && !reason.trim().isEmpty());

    return new CustomerStatusChangedEvent(
        eventId,
        customerId,
        oldStatus,
        newStatus,
        reason,
        changedBy,
        Instant.now(),
        source,
        metadata);
  }
}
