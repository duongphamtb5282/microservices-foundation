package com.pacific.payment.modules.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pacific.core.messaging.error.DeadLetterQueue;
import com.pacific.core.messaging.error.DlqSendException;
import com.pacific.core.messaging.error.ErrorClassifier;
import com.pacific.core.messaging.monitoring.KafkaMetrics;
import com.pacific.core.messaging.retry.BackoffStrategy;
import com.pacific.core.messaging.retry.RetryContext;
import com.pacific.core.messaging.retry.RetryPolicy;
import com.pacific.core.messaging.retry.RetryStrategy;
import com.pacific.payment.modules.consumer.event.OrderCancelledEvent;
import com.pacific.payment.modules.consumer.event.OrderCreatedEvent;
import com.pacific.payment.modules.consumer.exception.EventRetryableException;
import com.pacific.payment.modules.consumer.handler.OrderCancelledEventHandler;
import com.pacific.payment.modules.consumer.handler.OrderCreatedEventHandler;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for order events. The topic carries both ORDER_CREATED and ORDER_CANCELLED
 * payloads (ADR-0007), so the listener receives the raw JSON (String deserializer) and dispatches
 * on the eventType field — a fixed-target JsonDeserializer would silently deserialize a
 * cancellation into an OrderCreatedEvent with null fields and create a zero-amount payment. Retry
 * logic, metrics, and error handling come from backend-core.
 */
@Component
@Slf4j
public class OrderEventConsumer {

  private static final String EVENT_TYPE_CREATED = "ORDER_CREATED";
  private static final String EVENT_TYPE_CANCELLED = "ORDER_CANCELLED";

  private final OrderCreatedEventHandler createdHandler;
  private final OrderCancelledEventHandler cancelledHandler;
  private final RetryStrategy retryStrategy;
  private final ErrorClassifier errorClassifier;
  private final DeadLetterQueue deadLetterQueue;
  private final KafkaMetrics kafkaMetrics;
  private final BackoffStrategy backoffStrategy;
  private final ObjectMapper objectMapper;

  public OrderEventConsumer(
      OrderCreatedEventHandler createdHandler,
      OrderCancelledEventHandler cancelledHandler,
      RetryStrategy retryStrategy,
      ErrorClassifier errorClassifier,
      DeadLetterQueue deadLetterQueue,
      KafkaMetrics kafkaMetrics,
      BackoffStrategy backoffStrategy,
      ObjectMapper objectMapper) {
    this.createdHandler = createdHandler;
    this.cancelledHandler = cancelledHandler;
    this.retryStrategy = retryStrategy;
    this.errorClassifier = errorClassifier;
    this.deadLetterQueue = deadLetterQueue;
    this.kafkaMetrics = kafkaMetrics;
    this.backoffStrategy = backoffStrategy;
    this.objectMapper = objectMapper;
  }

  @KafkaListener(
      topics = "${payment.messaging.order-events-topic}",
      groupId = "${spring.kafka.consumer.group-id}",
      containerFactory = "kafkaListenerContainerFactory")
  public void consumeOrderEvent(
      @Payload String payload,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
      @Header(KafkaHeaders.OFFSET) long offset,
      Acknowledgment acknowledgment) {

    log.info("Received event from topic: {}, partition: {}, offset: {}", topic, partition, offset);

    JsonNode node;
    try {
      node = objectMapper.readTree(payload);
    } catch (Exception e) {
      // Unparseable payload — retrying cannot fix it. Classify (non-retryable -> DLQ) and ack only
      // after the DLQ send succeeds (T1); never poison the topic with an ack-loss or a dead loop.
      log.error("Unparseable order event payload at {}:{}-{}", topic, partition, offset);
      handleFailedEvent(
          RetryContext.builder()
              .eventId("unparseable-" + offset)
              .topic(topic)
              .partition(partition)
              .offset(offset)
              .startTime(java.time.Instant.now())
              .build(),
          e,
          acknowledgment);
      return;
    }

    processEventWithRetry(node, topic, partition, offset, acknowledgment);
  }

  /** Process event with retry logic using the injected retry strategy. */
  private void processEventWithRetry(
      JsonNode node, String topic, int partition, long offset, Acknowledgment acknowledgment) {
    String eventId = node.path("orderId").asText(null);
    if (eventId == null) {
      log.error("Order event without orderId at {}:{}-{}", topic, partition, offset);
      handleFailedEvent(
          RetryContext.builder()
              .eventId("no-order-id-" + offset)
              .topic(topic)
              .partition(partition)
              .offset(offset)
              .startTime(java.time.Instant.now())
              .build(),
          new IllegalArgumentException("Order event payload missing orderId"),
          acknowledgment);
      return;
    }

    String eventType = node.path("eventType").asText(EVENT_TYPE_CREATED);
    log.info(
        "Received event: {} from topic: {}, partition: {}, offset: {}, eventId: {}",
        eventType,
        topic,
        partition,
        offset,
        eventId);

    // Record consumption metrics
    kafkaMetrics.incrementEventsConsumed(topic, eventType);

    // Create retry context
    RetryContext context =
        RetryContext.builder()
            .eventId(eventId)
            .topic(topic)
            .partition(partition)
            .offset(offset)
            .startTime(java.time.Instant.now())
            .build();

    try {
      // Process with retry logic
      processWithRetry(node, context);

      // Acknowledge successful processing
      acknowledgment.acknowledge();

      long processingTime = System.currentTimeMillis() - context.getStartTime().toEpochMilli();
      log.info("Event processed successfully: {} in {}ms", eventId, processingTime);

      // Record success metrics
      kafkaMetrics.recordEventProcessingTime(topic, eventType, processingTime);

    } catch (Exception e) {
      log.error("Failed to process event: {} after retries", eventId, e);

      // Handle failed event based on error classification
      handleFailedEvent(context, e, acknowledgment);
    }
  }

  /**
   * Process the event with retry logic. The retry strategy rethrows the operation's original
   * exception (checked included) after classification; the caller's catch re-classifies it.
   */
  private void processWithRetry(JsonNode node, RetryContext context) throws Exception {
    RetryPolicy policy = createRetryPolicy(context);

    retryStrategy.executeWithRetry(
        () -> {
          try {
            processEvent(node);
            return true;
          } catch (Exception e) {
            log.warn(
                "Event processing failed for event: {}, attempt: {}",
                context.getEventId(),
                context.getAttemptCount() + 1,
                e);
            throw e;
          }
        },
        policy,
        context);
  }

  /**
   * Dispatch on the eventType field: ORDER_CREATED -> payment creation, ORDER_CANCELLED -> saga
   * compensation refund (ADR-0007). Both payloads share the orderId key, so per-partition ordering
   * guarantees the created event is resolved before the cancellation is delivered.
   */
  private void processEvent(JsonNode node) throws Exception {
    String eventType = node.path("eventType").asText("");
    if (EVENT_TYPE_CANCELLED.equals(eventType)) {
      OrderCancelledEvent cancelled = objectMapper.treeToValue(node, OrderCancelledEvent.class);
      cancelledHandler.handle(cancelled);
      log.info("Successfully processed order cancellation for order: {}", cancelled.getOrderId());
    } else {
      OrderCreatedEvent created = objectMapper.treeToValue(node, OrderCreatedEvent.class);
      createdHandler.handle(created);
      log.info("Successfully processed order event for order: {}", created.getOrderId());
    }
  }

  /** Handle failed event processing. */
  private void handleFailedEvent(
      RetryContext context, Exception e, Acknowledgment acknowledgment) {
    RetryPolicy retryPolicy = createRetryPolicy(context);
    if (errorClassifier.isRetryable(e, retryPolicy)) {
      // For retryable errors, don't acknowledge - let Kafka retry
      log.warn("Retryable error for event: {}, will retry", context.getEventId());
      throw new EventRetryableException("Retryable error for event: " + context.getEventId(), e);
    } else {
      // For non-retryable errors, send to DLQ and acknowledge
      log.error("Non-retryable error for event: {}, sending to DLQ", context.getEventId());
      sendToDeadLetterQueue(context, e);
      acknowledgment.acknowledge();
    }
  }

  /** Send failed event to Dead Letter Queue. */
  private void sendToDeadLetterQueue(RetryContext context, Exception e) {
    try {
      deadLetterQueue.send(context, e);
      log.info("Event sent to DLQ: {}", context.getEventId());
    } catch (Exception dlqException) {
      log.error("Failed to send event to DLQ: {}", context.getEventId(), dlqException);
      // Propagate the DLQ failure so the message is NOT acknowledged and is redelivered;
      // acknowledging here would permanently lose the event (at-least-once beats loss).
      // Mirrors backend-core BaseEventConsumer.sendToDeadLetterQueue.
      throw new DlqSendException(
          "Failed to send event to DLQ: " + context.getEventId(), dlqException);
    }
  }

  /** Create retry policy based on context and configuration. */
  private RetryPolicy createRetryPolicy(RetryContext context) {
    return RetryPolicy.builder()
        .maxAttempts(3)
        .initialBackoff(Duration.ofSeconds(1)) // Use fixed initial backoff
        .backoffMultiplier(2.0)
        .jitterFactor(0.1)
        .retryableExceptions(
            java.util.List.of(
                IllegalStateException.class,
                org.springframework.dao.DataAccessException.class,
                java.net.SocketTimeoutException.class))
        .build();
  }
}
