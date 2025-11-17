package com.pacific.order.infrastructure.eventsourcing;

import com.pacific.order.domain.event.OrderDomainEvent;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for event store operations
 */
public interface EventStoreRepository {

    /**
     * Save an event to the event store
     */
    void saveEvent(OrderDomainEvent event);

    /**
     * Get all events for an aggregate (order) in order
     */
    List<OrderDomainEvent> getEventsForAggregate(String aggregateId);

    /**
     * Get events for an aggregate from a specific version
     */
    List<OrderDomainEvent> getEventsForAggregateFromVersion(String aggregateId, int fromVersion);

    /**
     * Get the latest version for an aggregate
     */
    Optional<Integer> getLatestVersion(String aggregateId);

    /**
     * Check if aggregate exists
     */
    boolean aggregateExists(String aggregateId);

    /**
     * Get events by correlation ID for tracing
     */
    List<OrderDomainEvent> getEventsByCorrelationId(String correlationId);

    /**
     * Get events by user ID and event type
     */
    List<OrderDomainEvent> getEventsByUserIdAndType(String userId, String eventType);

    /**
     * Get events within a time range
     */
    List<OrderDomainEvent> getEventsInTimeRange(java.time.Instant startTime, java.time.Instant endTime);
}
