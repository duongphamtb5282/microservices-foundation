package com.pacific.payment.modules.consumer.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event representing order cancellation (ADR-0007 saga compensation). Consumed from the same
 * "order.events" topic as {@link OrderCreatedEvent}; the consumer dispatches on the eventType
 * field. Field names must match the producer's order-domain {@code OrderCancelledEvent}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCancelledEvent {

  private String orderId;
  private String userId;
  private String reason;
  private Instant eventTimestamp;
  private String correlationId;
  private String cancelledBy;
  private Integer version;
}
