package com.pacific.customer.domain.event;

import com.pacific.shared.messaging.cqrs.event.DomainEvent;

/** Base interface for customer domain events */
public interface CustomerDomainEvent extends DomainEvent {

  String getCustomerId();

  String getEventType();

  default String getAggregateId() {
    return getCustomerId();
  }
}
