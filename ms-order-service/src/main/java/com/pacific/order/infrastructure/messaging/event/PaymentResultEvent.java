package com.pacific.order.infrastructure.messaging.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payment outcome event consumed on the saga return path (F-02). Produced by ms-payment-service as
 * a JSON payload on {@code payment.events}; field names must match the producer's {@code
 * PaymentResultEvent} exactly. Unknown fields are ignored so the wire format may grow.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentResultEvent {

  private String paymentId;
  private String orderId;
  private String userId;

  /** "COMPLETED" or "FAILED" (PaymentStatus name from the producer). */
  private String status;

  private String transactionId;
  private String failureReason;
  private String correlationId;
  private String occurredOnIso;
}
