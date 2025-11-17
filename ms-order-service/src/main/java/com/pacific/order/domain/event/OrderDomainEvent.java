package com.pacific.order.domain.event;

import com.pacific.core.messaging.cqrs.event.DomainEvent;
import lombok.Data;

import java.time.Instant;

/**
 * Base interface for order domain events (Event Sourcing)
 */
public interface OrderDomainEvent extends DomainEvent {

    String getOrderId();
    String getUserId();
    Instant getEventTimestamp();
    String getEventType();
    String getCorrelationId();

    /**
     * Apply this event to the order aggregate.
     */
    void apply(OrderAggregate aggregate);
}
