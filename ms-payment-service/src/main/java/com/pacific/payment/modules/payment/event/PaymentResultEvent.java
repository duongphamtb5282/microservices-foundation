package com.pacific.payment.modules.payment.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pacific.shared.messaging.cqrs.event.DomainEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Payment outcome event published on the saga return path (F-02). Produced after the payment
 * transaction commits (via the payment outbox, ADR-0001 pattern) and consumed by ms-order-service,
 * which transitions the order to CONFIRMED (payment COMPLETED) or FAILED.
 *
 * <p>The timestamp travels as an ISO-8601 string ({@code occurredOnIso}) so the Kafka wire format
 * needs no JavaTimeModule — {@link #getOccurredOn()} parses it for the shared DomainEvent contract.
 */
public class PaymentResultEvent implements DomainEvent {

  private final String eventId;
  private final String paymentId;
  private final String orderId;
  private final String userId;

  /**
   * PaymentStatus name: "COMPLETED", "FAILED" or "REFUNDED" (string to keep the two services
   * decoupled).
   */
  private final String status;

  private final String transactionId;
  private final String failureReason;
  private final String correlationId;
  private final String occurredOnIso;

  @JsonCreator
  public PaymentResultEvent(
      @JsonProperty("eventId") String eventId,
      @JsonProperty("paymentId") String paymentId,
      @JsonProperty("orderId") String orderId,
      @JsonProperty("userId") String userId,
      @JsonProperty("status") String status,
      @JsonProperty("transactionId") String transactionId,
      @JsonProperty("failureReason") String failureReason,
      @JsonProperty("correlationId") String correlationId,
      @JsonProperty("occurredOnIso") String occurredOnIso) {
    this.eventId = eventId;
    this.paymentId = paymentId;
    this.orderId = orderId;
    this.userId = userId;
    this.status = status;
    this.transactionId = transactionId;
    this.failureReason = failureReason;
    this.correlationId = correlationId;
    this.occurredOnIso = occurredOnIso;
  }

  public static PaymentResultEvent completed(
      String paymentId, String orderId, String userId, String transactionId, String correlationId) {
    return new PaymentResultEvent(
        UUID.randomUUID().toString(),
        paymentId,
        orderId,
        userId,
        "COMPLETED",
        transactionId,
        null,
        correlationId,
        Instant.now().toString());
  }

  public static PaymentResultEvent failed(
      String paymentId, String orderId, String userId, String failureReason, String correlationId) {
    return new PaymentResultEvent(
        UUID.randomUUID().toString(),
        paymentId,
        orderId,
        userId,
        "FAILED",
        null,
        failureReason,
        correlationId,
        Instant.now().toString());
  }

  /**
   * Saga compensation return path (ADR-0007): the payment was refunded after the order was
   * cancelled. Consumed by the order service to record the refund outcome.
   */
  public static PaymentResultEvent refunded(
      String paymentId, String orderId, String userId, String transactionId, String correlationId) {
    return new PaymentResultEvent(
        UUID.randomUUID().toString(),
        paymentId,
        orderId,
        userId,
        "REFUNDED",
        transactionId,
        null,
        correlationId,
        Instant.now().toString());
  }

  public String getEventId() {
    return eventId;
  }

  public String getPaymentId() {
    return paymentId;
  }

  public String getOrderId() {
    return orderId;
  }

  public String getUserId() {
    return userId;
  }

  public String getStatus() {
    return status;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public String getOccurredOnIso() {
    return occurredOnIso;
  }

  @Override
  public String getEventType() {
    return "PAYMENT_RESULT";
  }

  @Override
  public Instant getOccurredOn() {
    return Instant.parse(occurredOnIso);
  }

  @Override
  public String getSource() {
    return "payment-service";
  }

  @Override
  public Map<String, Object> getMetadata() {
    return Map.of();
  }

  @Override
  public String getAggregateId() {
    return orderId;
  }
}
