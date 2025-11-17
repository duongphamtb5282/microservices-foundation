package com.pacific.order.domain.event;

import com.pacific.order.domain.model.OrderStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event representing order cancellation (Event Sourcing)
 */
@Data
@Builder
public class OrderCancelledEvent implements OrderDomainEvent {

    @lombok.Getter
    @JsonProperty("orderId")
    private final String orderId;

    @JsonProperty("userId")
    private final String userId;

    @JsonProperty("reason")
    private final String reason;

    @JsonProperty("eventTimestamp")
    private final Instant eventTimestamp;

    @JsonProperty("correlationId")
    private final String correlationId;

    @JsonProperty("cancelledBy")
    private final String cancelledBy;

    @JsonProperty("version")
    private final Integer version;

    @JsonCreator
    public OrderCancelledEvent(
            @JsonProperty("orderId") String orderId,
            @JsonProperty("userId") String userId,
            @JsonProperty("reason") String reason,
            @JsonProperty("eventTimestamp") Instant eventTimestamp,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("cancelledBy") String cancelledBy,
            @JsonProperty("version") Integer version) {
        this.orderId = orderId;
        this.userId = userId;
        this.reason = reason;
        this.eventTimestamp = eventTimestamp;
        this.correlationId = correlationId;
        this.cancelledBy = cancelledBy;
        this.version = version;
    }

    @Override
    public String getEventId() {
        return orderId != null ? "ORDER_CANCELLED_" + orderId : java.util.UUID.randomUUID().toString();
    }

    @Override
    public String getEventType() {
        return "ORDER_CANCELLED";
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
    public String getAggregateId() {
        return orderId;
    }





    @Override
    public String getSource() {
        return "order-service";
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", userId);
        metadata.put("reason", reason);
        metadata.put("cancelledBy", cancelledBy);
        return metadata;
    }

    @Override
    public void apply(OrderAggregate aggregate) {
        aggregate.applyOrderCancelled(this);
    }
}
