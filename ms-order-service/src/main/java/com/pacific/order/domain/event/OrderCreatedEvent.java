package com.pacific.order.domain.event;

import com.pacific.order.domain.model.OrderItem;
import com.pacific.order.domain.model.Money;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Domain event representing order creation
 * Published to Kafka when an order is successfully created
 */
@Data
@Builder
public class OrderCreatedEvent implements com.pacific.core.messaging.cqrs.event.DomainEvent {
    
    private String orderId;
    private String userId;
    private List<OrderItem> items;
    private Money totalAmount;
    private Instant timestamp;
    private String correlationId;

    @Override
    public String getEventId() {
        return null;
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

    /**
     * Get total amount as BigDecimal for serialization
     */
    public BigDecimal getTotalAmountValue() {
        return totalAmount != null ? totalAmount.getAmount() : BigDecimal.ZERO;
    }

    /**
     * Get currency code for serialization
     */
    public String getCurrencyCode() {
        return totalAmount != null ? totalAmount.getCurrency().getCurrencyCode() : "USD";
    }
}

