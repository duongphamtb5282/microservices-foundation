package com.pacific.order.infrastructure.eventsourcing.repository;

import com.pacific.order.infrastructure.eventsourcing.entity.OrderEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA Repository for OrderEventEntity
 */
@Repository
public interface OrderEventJpaRepository extends JpaRepository<OrderEventEntity, String> {

    /**
     * Find all events for an aggregate (order) ordered by version
     */
    @Query("SELECT e FROM OrderEventEntity e WHERE e.orderId = :orderId ORDER BY e.version ASC")
    List<OrderEventEntity> findByOrderIdOrderByVersionAsc(@Param("orderId") String orderId);

    /**
     * Find events for an aggregate from a specific version
     */
    @Query("SELECT e FROM OrderEventEntity e WHERE e.orderId = :orderId AND e.version >= :fromVersion ORDER BY e.version ASC")
    List<OrderEventEntity> findByOrderIdFromVersionOrderByVersionAsc(@Param("orderId") String orderId, @Param("fromVersion") int fromVersion);

    /**
     * Get the latest version for an aggregate
     */
    @Query("SELECT MAX(e.version) FROM OrderEventEntity e WHERE e.orderId = :orderId")
    Integer findMaxVersionByOrderId(@Param("orderId") String orderId);

    /**
     * Check if aggregate exists
     */
    @Query("SELECT COUNT(e) > 0 FROM OrderEventEntity e WHERE e.orderId = :orderId")
    boolean existsByOrderId(@Param("orderId") String orderId);

    /**
     * Find events by correlation ID
     */
    List<OrderEventEntity> findByCorrelationIdOrderByEventTimestampAsc(String correlationId);

    /**
     * Find events by user ID and event type
     */
    List<OrderEventEntity> findByUserIdAndEventTypeOrderByEventTimestampDesc(String userId, String eventType);

    /**
     * Find events within a time range
     */
    @Query("SELECT e FROM OrderEventEntity e WHERE e.eventTimestamp BETWEEN :startTime AND :endTime ORDER BY e.eventTimestamp ASC")
    List<OrderEventEntity> findByEventTimestampBetweenOrderByEventTimestampAsc(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    /**
     * Count events by event type for metrics
     */
    @Query("SELECT COUNT(e) FROM OrderEventEntity e WHERE e.eventType = :eventType")
    long countByEventType(@Param("eventType") String eventType);

    /**
     * Find events by user ID (for user activity)
     */
    @Query("SELECT e FROM OrderEventEntity e WHERE e.userId = :userId ORDER BY e.eventTimestamp DESC")
    List<OrderEventEntity> findByUserIdOrderByEventTimestampDesc(@Param("userId") String userId);
}
