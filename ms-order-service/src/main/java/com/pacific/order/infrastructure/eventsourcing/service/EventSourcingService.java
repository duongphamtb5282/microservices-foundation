package com.pacific.order.infrastructure.eventsourcing.service;

import com.pacific.order.domain.event.OrderAggregate;
import com.pacific.order.domain.event.OrderDomainEvent;
import com.pacific.order.infrastructure.eventsourcing.EventStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for event sourcing operations on Order aggregates.
 * Provides capabilities to rebuild aggregates from events and query event data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventSourcingService {

    private final EventStoreRepository eventStoreRepository;

    /**
     * Rebuild OrderAggregate from events in the event store.
     *
     * @param orderId The order ID to rebuild
     * @return Rebuilt aggregate or empty if not found
     */
    public Optional<OrderAggregate> rebuildAggregate(String orderId) {
        try {
            if (!eventStoreRepository.aggregateExists(orderId)) {
                log.debug("Aggregate not found: {}", orderId);
                return Optional.empty();
            }

            List<OrderDomainEvent> events = eventStoreRepository.getEventsForAggregate(orderId);

            if (events.isEmpty()) {
                log.debug("No events found for aggregate: {}", orderId);
                return Optional.empty();
            }

            OrderAggregate aggregate = rebuildFromEvents(events);

            log.debug("Rebuilt aggregate: {} with {} events", orderId, events.size());
            return Optional.of(aggregate);

        } catch (Exception e) {
            log.error("Failed to rebuild aggregate: {}", orderId, e);
            return Optional.empty();
        }
    }

    /**
     * Rebuild aggregate from a specific version.
     */
    public Optional<OrderAggregate> rebuildAggregateFromVersion(String orderId, int fromVersion) {
        try {
            List<OrderDomainEvent> events = eventStoreRepository.getEventsForAggregateFromVersion(orderId, fromVersion);

            if (events.isEmpty()) {
                log.debug("No events found for aggregate: {} from version: {}", orderId, fromVersion);
                return Optional.empty();
            }

            OrderAggregate aggregate = rebuildFromEvents(events);

            log.debug("Rebuilt aggregate: {} from version: {} with {} events", orderId, fromVersion, events.size());
            return Optional.of(aggregate);

        } catch (Exception e) {
            log.error("Failed to rebuild aggregate: {} from version: {}", orderId, fromVersion, e);
            return Optional.empty();
        }
    }

    /**
     * Get all events for an order with pagination.
     */
    public EventSourcingResult getOrderEvents(String orderId, int page, int size) {
        try {
            List<OrderDomainEvent> allEvents = eventStoreRepository.getEventsForAggregate(orderId);

            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, allEvents.size());

            List<OrderDomainEvent> pageEvents = allEvents.subList(startIndex, endIndex);

            return EventSourcingResult.builder()
                .events(pageEvents)
                .totalEvents(allEvents.size())
                .page(page)
                .size(size)
                .totalPages((allEvents.size() + size - 1) / size)
                .hasNext(endIndex < allEvents.size())
                .hasPrevious(page > 0)
                .build();

        } catch (Exception e) {
            log.error("Failed to get order events: {}", orderId, e);
            return EventSourcingResult.empty();
        }
    }

    /**
     * Get events by correlation ID for request tracing.
     */
    public List<OrderDomainEvent> getEventsByCorrelationId(String correlationId) {
        return eventStoreRepository.getEventsByCorrelationId(correlationId);
    }

    /**
     * Get user activity events.
     */
    public List<OrderDomainEvent> getUserActivity(String userId, String eventType) {
        return eventStoreRepository.getEventsByUserIdAndType(userId, eventType);
    }

    /**
     * Get events in time range for analytics.
     */
    public List<OrderDomainEvent> getEventsInTimeRange(Instant startTime, Instant endTime) {
        return eventStoreRepository.getEventsInTimeRange(startTime, endTime);
    }

    /**
     * Get event statistics for monitoring.
     */
    public EventStatistics getEventStatistics() {
        // This would typically query the database for statistics
        // For now, return basic stats
        return EventStatistics.builder()
            .totalEvents(0L) // Would be calculated from database
            .eventsByType(java.util.Map.of()) // Would be calculated from database
            .build();
    }

    /**
     * Rebuild aggregate from list of events.
     */
    private OrderAggregate rebuildFromEvents(List<OrderDomainEvent> events) {
        OrderAggregate aggregate = new OrderAggregate();

        for (OrderDomainEvent event : events) {
            event.apply(aggregate);
        }

        return aggregate;
    }

    /**
     * Result for event sourcing queries.
     */
    public static class EventSourcingResult {
        private final List<OrderDomainEvent> events;
        private final int totalEvents;
        private final int page;
        private final int size;
        private final int totalPages;
        private final boolean hasNext;
        private final boolean hasPrevious;

        public EventSourcingResult(List<OrderDomainEvent> events, int totalEvents, int page, int size,
                                 int totalPages, boolean hasNext, boolean hasPrevious) {
            this.events = events;
            this.totalEvents = totalEvents;
            this.page = page;
            this.size = size;
            this.totalPages = totalPages;
            this.hasNext = hasNext;
            this.hasPrevious = hasPrevious;
        }

        public static EventSourcingResult empty() {
            return new EventSourcingResult(List.of(), 0, 0, 10, 0, false, false);
        }

        public static EventSourcingResultBuilder builder() {
            return new EventSourcingResultBuilder();
        }

        // Getters
        public List<OrderDomainEvent> getEvents() { return events; }
        public int getTotalEvents() { return totalEvents; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public int getTotalPages() { return totalPages; }
        public boolean isHasNext() { return hasNext; }
        public boolean isHasPrevious() { return hasPrevious; }

        public static class EventSourcingResultBuilder {
            private List<OrderDomainEvent> events = List.of();
            private int totalEvents = 0;
            private int page = 0;
            private int size = 10;
            private int totalPages = 0;
            private boolean hasNext = false;
            private boolean hasPrevious = false;

            public EventSourcingResultBuilder events(List<OrderDomainEvent> events) {
                this.events = events;
                return this;
            }

            public EventSourcingResultBuilder totalEvents(int totalEvents) {
                this.totalEvents = totalEvents;
                return this;
            }

            public EventSourcingResultBuilder page(int page) {
                this.page = page;
                return this;
            }

            public EventSourcingResultBuilder size(int size) {
                this.size = size;
                return this;
            }

            public EventSourcingResultBuilder totalPages(int totalPages) {
                this.totalPages = totalPages;
                return this;
            }

            public EventSourcingResultBuilder hasNext(boolean hasNext) {
                this.hasNext = hasNext;
                return this;
            }

            public EventSourcingResultBuilder hasPrevious(boolean hasPrevious) {
                this.hasPrevious = hasPrevious;
                return this;
            }

            public EventSourcingResult build() {
                return new EventSourcingResult(events, totalEvents, page, size, totalPages, hasNext, hasPrevious);
            }
        }
    }

    /**
     * Event statistics for monitoring.
     */
    public static class EventStatistics {
        private final long totalEvents;
        private final java.util.Map<String, Long> eventsByType;

        public EventStatistics(long totalEvents, java.util.Map<String, Long> eventsByType) {
            this.totalEvents = totalEvents;
            this.eventsByType = eventsByType;
        }

        public long getTotalEvents() { return totalEvents; }
        public java.util.Map<String, Long> getEventsByType() { return eventsByType; }

        public static EventStatisticsBuilder builder() {
            return new EventStatisticsBuilder();
        }

        public static class EventStatisticsBuilder {
            private long totalEvents = 0;
            private java.util.Map<String, Long> eventsByType = new java.util.HashMap<>();

            public EventStatisticsBuilder totalEvents(long totalEvents) {
                this.totalEvents = totalEvents;
                return this;
            }

            public EventStatisticsBuilder eventsByType(java.util.Map<String, Long> eventsByType) {
                this.eventsByType = eventsByType;
                return this;
            }

            public EventStatistics build() {
                return new EventStatistics(totalEvents, eventsByType);
            }
        }
    }
}
