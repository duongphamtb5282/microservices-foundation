package com.pacific.order.domain.event;

import com.pacific.order.domain.event.OrderDomainEvent;
import com.pacific.order.domain.model.Money;
import com.pacific.order.domain.model.OrderItem;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Event representing order creation (Event Sourcing version)
 */
@Data
@Builder
public class OrderCreatedEventV2 implements OrderDomainEvent {

    @JsonProperty("orderId")
    private final String orderId;

    @JsonProperty("userId")
    private final String userId;

    @JsonProperty("items")
    private final List<OrderItem> items;

    @JsonProperty("totalAmount")
    private final Money totalAmount;

    @JsonProperty("eventTimestamp")
    private final Instant eventTimestamp;

    @JsonProperty("correlationId")
    private final String correlationId;

    @JsonProperty("createdBy")
    private final String createdBy;

    @JsonProperty("version")
    private final Integer version;

    @JsonCreator
    public OrderCreatedEventV2(
            @JsonProperty("orderId") String orderId,
            @JsonProperty("userId") String userId,
            @JsonProperty("items") List<OrderItem> items,
            @JsonProperty("totalAmount") Money totalAmount,
            @JsonProperty("eventTimestamp") Instant eventTimestamp,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("createdBy") String createdBy,
            @JsonProperty("version") Integer version) {
        this.orderId = orderId;
        this.userId = userId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.eventTimestamp = eventTimestamp;
        this.correlationId = correlationId;
        this.createdBy = createdBy;
        this.version = version;
    }

    @Override
    public String getEventId() {
        return orderId != null ? "ORDER_CREATED_" + orderId : UUID.randomUUID().toString();
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
        return eventTimestamp;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public void apply(OrderAggregate aggregate) {

    }

    @Override
    public String getSource() {
        return "order-service";
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", userId);
        metadata.put("totalAmount", totalAmount != null ? totalAmount.getAmount() : null);
        metadata.put("itemsCount", items != null ? items.size() : 0);
        return metadata;
    }
}
