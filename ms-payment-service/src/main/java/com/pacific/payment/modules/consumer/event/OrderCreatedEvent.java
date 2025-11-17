package com.pacific.payment.modules.consumer.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event representing order creation
 * This is consumed from Kafka topic "order.events"
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCreatedEvent {
    
    private String orderId;
    private String userId;
    private BigDecimal totalAmountValue;  // Maps to totalAmount.amount
    private String currencyCode;           // Maps to totalAmount.currency
    private Instant timestamp;
    private String correlationId;

    /**
     * Get total amount as BigDecimal
     */
    public BigDecimal getTotalAmount() {
        return totalAmountValue != null ? totalAmountValue : BigDecimal.ZERO;
    }
}

