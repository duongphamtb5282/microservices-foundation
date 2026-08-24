package com.pacific.order.infrastructure.eventsourcing.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** JPA Entity for Order Events (Event Sourcing) */
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

  // columnDefinition only drives DDL generation (Liquibase owns the schema) — without
  // @JdbcTypeCode the JDBC bind stays VARCHAR and PostgreSQL rejects it with 42804 "column
  // event_data is of type json but expression is of type character varying". JSON binds the
  // String payload as json (Hibernate 6 passes String through verbatim).
  @Column(name = "event_data", columnDefinition = "JSON", nullable = false)
  @JdbcTypeCode(SqlTypes.JSON)
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
