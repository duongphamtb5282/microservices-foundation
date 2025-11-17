package com.pacific.order.domain.event;

import com.pacific.order.domain.model.OrderStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Event representing order status update (Event Sourcing)
 */
@Data
@Builder
public class OrderStatusUpdatedEvent implements OrderDomainEvent {

    @JsonProperty("orderId")
    private final String orderId;

    @JsonProperty("userId")
    private final String userId;

    @JsonProperty("oldStatus")
    private final OrderStatus oldStatus;

    @JsonProperty("newStatus")
    private final OrderStatus newStatus;

    @JsonProperty("eventTimestamp")
    private final Instant eventTimestamp;

    @JsonProperty("correlationId")
    private final String correlationId;

    @JsonProperty("updatedBy")
    private final String updatedBy;

    @JsonProperty("version")
    private final Integer version;

    @JsonCreator
    public OrderStatusUpdatedEvent(
            @JsonProperty("orderId") String orderId,
            @JsonProperty("userId") String userId,
            @JsonProperty("oldStatus") OrderStatus oldStatus,
            @JsonProperty("newStatus") OrderStatus newStatus,
            @JsonProperty("eventTimestamp") Instant eventTimestamp,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("updatedBy") String updatedBy,
            @JsonProperty("version") Integer version) {
        this.orderId = orderId;
        this.userId = userId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.eventTimestamp = eventTimestamp;
        this.correlationId = correlationId;
        this.updatedBy = updatedBy;
        this.version = version;
    }

    @Override
    public String getEventId() {
        return null;
    }

    @Override
    public String getEventType() {
        return "ORDER_STATUS_UPDATED";
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

    @Override
    public void apply(OrderAggregate aggregate) {
        aggregate.applyOrderStatusUpdated(this);
    }
}
