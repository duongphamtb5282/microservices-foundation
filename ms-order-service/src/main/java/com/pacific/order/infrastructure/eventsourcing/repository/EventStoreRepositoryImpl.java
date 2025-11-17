package com.pacific.order.infrastructure.eventsourcing.repository;

import com.pacific.order.domain.event.OrderDomainEvent;
import com.pacific.order.infrastructure.eventsourcing.EventStoreRepository;
import com.pacific.order.infrastructure.eventsourcing.entity.OrderEventEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JPA implementation of EventStoreRepository
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class EventStoreRepositoryImpl implements EventStoreRepository {

    private final OrderEventJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void saveEvent(OrderDomainEvent event) {
        try {
            OrderEventEntity entity = OrderEventEntity.builder()
                .id(UUID.randomUUID().toString())
                .orderId(event.getAggregateId())
                .eventType(event.getEventType())
                .eventData(objectMapper.writeValueAsString(event))
                .eventTimestamp(LocalDateTime.ofInstant(event.getOccurredOn(), ZoneOffset.UTC))
                .correlationId(event.getCorrelationId())
                .userId(extractUserId(event))
                .version(extractVersion(event))
                .createdBy("SYSTEM") // Could be enhanced to get from security context
                .build();

            jpaRepository.save(entity);

            log.debug("Saved event: {} for aggregate: {}", event.getEventType(), event.getAggregateId());

        } catch (Exception e) {
            log.error("Failed to save event: {}", event.getEventType(), e);
            throw new RuntimeException("Failed to save event", e);
        }
    }

    @Override
    public List<OrderDomainEvent> getEventsForAggregate(String aggregateId) {
        try {
            List<OrderEventEntity> entities = jpaRepository.findByOrderIdOrderByVersionAsc(aggregateId);
            return entities.stream()
                .map(this::convertToDomainEvent)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get events for aggregate: {}", aggregateId, e);
            throw new RuntimeException("Failed to get events for aggregate", e);
        }
    }

    @Override
    public List<OrderDomainEvent> getEventsForAggregateFromVersion(String aggregateId, int fromVersion) {
        try {
            List<OrderEventEntity> entities = jpaRepository.findByOrderIdFromVersionOrderByVersionAsc(aggregateId, fromVersion);
            return entities.stream()
                .map(this::convertToDomainEvent)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get events for aggregate: {} from version: {}", aggregateId, fromVersion, e);
            throw new RuntimeException("Failed to get events for aggregate from version", e);
        }
    }

    @Override
    public Optional<Integer> getLatestVersion(String aggregateId) {
        try {
            Integer version = jpaRepository.findMaxVersionByOrderId(aggregateId);
            return Optional.ofNullable(version);
        } catch (Exception e) {
            log.error("Failed to get latest version for aggregate: {}", aggregateId, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean aggregateExists(String aggregateId) {
        return jpaRepository.existsByOrderId(aggregateId);
    }

    @Override
    public List<OrderDomainEvent> getEventsByCorrelationId(String correlationId) {
        try {
            List<OrderEventEntity> entities = jpaRepository.findByCorrelationIdOrderByEventTimestampAsc(correlationId);
            return entities.stream()
                .map(this::convertToDomainEvent)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get events for correlation ID: {}", correlationId, e);
            return List.of();
        }
    }

    @Override
    public List<OrderDomainEvent> getEventsByUserIdAndType(String userId, String eventType) {
        try {
            List<OrderEventEntity> entities = jpaRepository.findByUserIdAndEventTypeOrderByEventTimestampDesc(userId, eventType);
            return entities.stream()
                .map(this::convertToDomainEvent)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get events for user: {} and type: {}", userId, eventType, e);
            return List.of();
        }
    }

    @Override
    public List<OrderDomainEvent> getEventsInTimeRange(java.time.Instant startTime, java.time.Instant endTime) {
        try {
            LocalDateTime start = LocalDateTime.ofInstant(startTime, ZoneOffset.UTC);
            LocalDateTime end = LocalDateTime.ofInstant(endTime, ZoneOffset.UTC);

            List<OrderEventEntity> entities = jpaRepository.findByEventTimestampBetweenOrderByEventTimestampAsc(start, end);
            return entities.stream()
                .map(this::convertToDomainEvent)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get events in time range: {} to {}", startTime, endTime, e);
            return List.of();
        }
    }

    /**
     * Convert entity to domain event using Jackson.
     */
    @SuppressWarnings("unchecked")
    private OrderDomainEvent convertToDomainEvent(OrderEventEntity entity) {
        try {
            // Deserialize based on event type
            Class<?> eventClass = getEventClass(entity.getEventType());
            return (OrderDomainEvent) objectMapper.readValue(entity.getEventData(), eventClass);
        } catch (Exception e) {
            log.error("Failed to convert entity to domain event: {}", entity.getEventType(), e);
            throw new RuntimeException("Failed to convert entity to domain event", e);
        }
    }

    /**
     * Get event class based on event type.
     */
    private Class<?> getEventClass(String eventType) {
        return switch (eventType) {
            case "ORDER_CREATED" -> com.pacific.order.domain.event.OrderCreatedEventV2.class;
            case "ORDER_CANCELLED" -> com.pacific.order.domain.event.OrderCancelledEvent.class;
            case "ORDER_STATUS_UPDATED" -> com.pacific.order.domain.event.OrderStatusUpdatedEvent.class;
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }

    /**
     * Extract user ID from event (fallback to system if not available).
     */
    private String extractUserId(OrderDomainEvent event) {
        try {
            return event.getClass().getMethod("getUserId").invoke(event).toString();
        } catch (Exception e) {
            return "SYSTEM";
        }
    }

    /**
     * Extract version from event (default to 1 if not available).
     */
    private Integer extractVersion(OrderDomainEvent event) {
        try {
            return (Integer) event.getClass().getMethod("getVersion").invoke(event);
        } catch (Exception e) {
            return 1;
        }
    }
}
