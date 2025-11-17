package com.pacific.order.infrastructure.eventsourcing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA Entity for Order Events (Event Sourcing)
 */
@Entity
@Table(name = "order_events")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderEventEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "order_id", length = 36, nullable = false)
    private String orderId;

    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;

    @Column(name = "event_data", columnDefinition = "JSON", nullable = false)
    private String eventData;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "version", nullable = false)
    private Integer version;

    // Audit fields
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100, nullable = false)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (eventTimestamp == null) {
            eventTimestamp = LocalDateTime.now();
        }
    }
}
