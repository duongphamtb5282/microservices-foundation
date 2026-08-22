package com.pacific.order.domain.event;

import com.pacific.order.domain.model.Money;
import com.pacific.order.domain.model.OrderItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Domain event representing order creation Published to Kafka when an order is successfully created
 */
@Data
@Builder
public class OrderCreatedEvent implements com.pacific.shared.messaging.cqrs.event.DomainEvent {

  private String orderId;
  private String userId;
  private List<OrderItem> items;
  private Money totalAmount;
  private Instant timestamp;
  private String correlationId;

  @Override
  public String getEventId() {
    // One ORDER_CREATED event per order — the orderId doubles as the event id (used in Kafka
    // headers; KafkaEventPublisher requires a non-null event id)
    return orderId;
  }

  @Override
  public String getEventType() {
    return "ORDER_CREATED";
  }

  @Override
  public String getAggregateId() {
    return orderId;
  }

  @Override
  public Instant getOccurredOn() {
    return timestamp != null ? timestamp : Instant.now();
  }

  @Override
  public String getSource() {
    return null;
  }

  @Override
  public String getCorrelationId() {
    return correlationId;
  }

  @Override
  public Map<String, Object> getMetadata() {
    return null;
  }

  /** Get total amount as BigDecimal for serialization */
  public BigDecimal getTotalAmountValue() {
    return totalAmount != null ? totalAmount.getAmount() : BigDecimal.ZERO;
  }

  /** Get currency code for serialization */
  public String getCurrencyCode() {
    return totalAmount != null ? totalAmount.getCurrency().getCurrencyCode() : "USD";
  }
}
