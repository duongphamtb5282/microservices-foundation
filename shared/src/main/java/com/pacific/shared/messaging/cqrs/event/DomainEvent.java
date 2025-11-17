package com.pacific.shared.messaging.cqrs.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/** Base class for all domain events. Events represent something that has happened in the domain. */
public interface DomainEvent extends Serializable {

  /** Unique event identifier */
  String getEventId();

  /** Event type/name */
  String getEventType();

  /** Timestamp when event occurred */
  Instant getOccurredOn();

  /** Source system/service that generated the event */
  String getSource();

  /** Correlation ID for distributed tracing */
  String getCorrelationId();

  /** Additional metadata */
  Map<String, Object> getMetadata();

  /**
   * Get the aggregate ID associated with this event. Subclasses should override this method to
   * provide the appropriate aggregate ID.
   *
   * @return the aggregate ID, or null if not applicable
   */
  String getAggregateId();
}
