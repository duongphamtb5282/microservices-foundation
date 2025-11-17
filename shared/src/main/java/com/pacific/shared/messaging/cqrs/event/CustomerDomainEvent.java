package com.pacific.shared.messaging.cqrs.event;

/** Common interface for Customer Domain Events - shared across all microservices */
public interface CustomerDomainEvent extends DomainEvent {

  /** Gets the customer ID associated with this event */
  String getCustomerId();

  /** Gets the event type */
  String getEventType();

  @Override
  default String getAggregateId() {
    return getCustomerId();
  }
}
