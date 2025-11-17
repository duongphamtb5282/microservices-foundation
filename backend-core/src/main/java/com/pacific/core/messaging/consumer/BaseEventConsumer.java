package com.pacific.core.messaging.consumer;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import com.pacific.core.messaging.config.KafkaWrapperProperties;
import com.pacific.core.messaging.error.DeadLetterQueue;
import com.pacific.core.messaging.error.ErrorClassifier;
import com.pacific.core.messaging.monitoring.KafkaMetrics;
import com.pacific.core.messaging.retry.BackoffStrategy;
import com.pacific.core.messaging.retry.MaxRetriesExceededException;
import com.pacific.core.messaging.retry.RetryContext;
import com.pacific.core.messaging.retry.RetryPolicy;
import com.pacific.core.messaging.retry.RetryStrategy;

/**
 * Base implementation for Kafka event consumers with built-in retry logic, metrics collection, and
 * error handling using backend-core components.
 *
 * <p>This class provides: - Automatic retry with exponential backoff - Dead Letter Queue (DLQ)
 * support - Metrics collection - Error classification - Idempotent processing
 *
 * @param <T> The event type to consume
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseEventConsumer<T> {

  protected final RetryStrategy retryStrategy;
  protected final ErrorClassifier errorClassifier;
  protected final DeadLetterQueue deadLetterQueue;
  protected final KafkaMetrics kafkaMetrics;
  protected final BackoffStrategy backoffStrategy;
  protected final KafkaWrapperProperties properties;

  // Default retry policy
  protected final RetryPolicy defaultRetryPolicy = RetryPolicy.defaultPolicy();

  // Processing counters for idempotency
  private final AtomicLong processedEvents = new AtomicLong(0);
  private final AtomicLong failedEvents = new AtomicLong(0);

  /**
   * Handle incoming Kafka event with full error handling and retry logic. This is the main entry
   * point that should be called from @KafkaListener methods.
   *
   * @param event The event payload
   * @param topic The Kafka topic name
   * @param partition The partition number
   * @param offset The offset in partition
   * @param acknowledgment Kafka acknowledgment for manual commit
   * @throws Exception if a retryable error occurs that should be handled by Kafka
   */
  public void handleEvent(
      @Payload T event,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
      @Header(KafkaHeaders.OFFSET) long offset,
      Acknowledgment acknowledgment)
      throws Exception {

    String eventId = generateEventId(event);
    String correlationId = extractCorrelationId(event);

    log.info(
        "Received event: {} from topic: {}, partition: {}, offset: {}, eventId: {}",
        event.getClass().getSimpleName(),
        topic,
        partition,
        offset,
        eventId);

    // Record consumption metrics
    kafkaMetrics.incrementEventsConsumed(topic, event.getClass().getSimpleName());

    // Create retry context
    RetryContext context =
        RetryContext.builder()
            .eventId(eventId)
            .topic(topic)
            .partition(partition)
            .offset(offset)
            .correlationId(correlationId)
            .startTime(Instant.now())
            .build();

    try {
      // Check if already processed (idempotency)
      if (isAlreadyProcessed(eventId)) {
        log.debug("Event already processed: {}", eventId);
        acknowledgment.acknowledge();
        return;
      }

      // Process with retry logic
      processWithRetry(event, context);

      // Mark as processed
      markAsProcessed(eventId);

      // Acknowledge successful processing
      acknowledgment.acknowledge();

      long processingTime = System.currentTimeMillis() - context.getStartTime().toEpochMilli();
      log.info("Event processed successfully: {} in {}ms", eventId, processingTime);

      // Record success metrics
      kafkaMetrics.recordEventProcessingTime(
          topic, event.getClass().getSimpleName(), processingTime);

    } catch (Exception e) {
      log.error("Failed to process event: {} after retries", eventId, e);

      failedEvents.incrementAndGet();
      kafkaMetrics.incrementFailedEvents(topic, event.getClass().getSimpleName());

      // Handle failed event based on error classification
      try {
        handleFailedEvent(event, context, e, acknowledgment);
      } catch (Exception retryException) {
        // Re-throw retryable exceptions to let Kafka handle retries
        throw retryException;
      }
    }
  }

  /**
   * Process the event with retry logic. This method will attempt to process the event according to
   * the retry policy.
   */
  private void processWithRetry(T event, RetryContext context) {
    RetryPolicy policy = createRetryPolicy(context);

    try {
      retryStrategy.executeWithRetry(
          () -> {
            try {
              processEvent(event);
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
    } catch (MaxRetriesExceededException e) {
      throw new RuntimeException("Max retries exceeded for event: " + context.getEventId(), e);
    }
  }

  /**
   * Handle failed event processing. Either retry or send to Dead Letter Queue based on error
   * classification.
   */
  private void handleFailedEvent(
      T event, RetryContext context, Exception e, Acknowledgment acknowledgment) throws Exception {
    if (errorClassifier.isRetryable(e, defaultRetryPolicy)) {
      // For retryable errors, don't acknowledge - let Kafka retry
      log.warn("Retryable error for event: {}, will retry", context.getEventId());
      throw e; // This will cause the message to be reprocessed
    } else {
      // For non-retryable errors, send to DLQ and acknowledge
      log.error("Non-retryable error for event: {}, sending to DLQ", context.getEventId());
      sendToDeadLetterQueue(event, context, e);
      acknowledgment.acknowledge();
    }
  }

  /** Send failed event to Dead Letter Queue. */
  private void sendToDeadLetterQueue(T event, RetryContext context, Exception e) {
    try {
      // Store the original event in context for DLQ
      context.setOriginalPayload(event);
      deadLetterQueue.send(context, e);
      log.info("Event sent to DLQ: {}", context.getEventId());
    } catch (Exception dlqException) {
      log.error("Failed to send event to DLQ: {}", context.getEventId(), dlqException);
      // In production, you might want to implement a fallback mechanism here
    }
  }

  /** Create retry policy based on context and configuration. */
  private RetryPolicy createRetryPolicy(RetryContext context) {
    KafkaWrapperProperties.Retry retryConfig = properties.getRetry();
    return RetryPolicy.builder()
        .maxAttempts(retryConfig.getMaxAttempts())
        .initialBackoff(retryConfig.getInitialBackoff())
        .maxBackoff(retryConfig.getMaxBackoff())
        .backoffMultiplier(retryConfig.getBackoffMultiplier())
        .jitterFactor(retryConfig.getJitterFactor())
        .retryableExceptions(
            retryConfig.getRetryableExceptions().isEmpty()
                ? java.util.Arrays.asList(
                    IllegalStateException.class,
                    org.springframework.dao.DataAccessException.class,
                    java.net.SocketTimeoutException.class)
                : retryConfig.getRetryableExceptions().stream().map(this::classForName).toList())
        .nonRetryableExceptions(
            retryConfig.getNonRetryableExceptions().stream().map(this::classForName).toList())
        .build();
  }

  /** Convert class name string to Class object. */
  @SuppressWarnings("unchecked")
  private Class<? extends Exception> classForName(String className) {
    try {
      return (Class<? extends Exception>) Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Invalid exception class: " + className, e);
    }
  }

  /** Generate unique event ID for idempotency tracking. */
  private String generateEventId(T event) {
    // Try to extract ID from event if it has one
    try {
      return event.getClass().getMethod("getEventId").invoke(event).toString();
    } catch (Exception e) {
      // Fallback to UUID based on event content
      return UUID.nameUUIDFromBytes(event.toString().getBytes()).toString();
    }
  }

  /** Extract correlation ID from event if available. */
  private String extractCorrelationId(T event) {
    try {
      return event.getClass().getMethod("getCorrelationId").invoke(event).toString();
    } catch (Exception e) {
      return null;
    }
  }

  /** Check if event was already processed (idempotency check). */
  private boolean isAlreadyProcessed(String eventId) {
    // In a production system, you would check a persistent store (Redis, database)
    // For now, we'll use a simple in-memory check
    return false;
  }

  /** Mark event as processed. */
  private void markAsProcessed(String eventId) {
    processedEvents.incrementAndGet();
    // In production, store this in Redis or database
  }

  /**
   * Abstract method that subclasses must implement to process the actual event.
   *
   * @param event The event to process
   */
  protected abstract void processEvent(T event);

  /** Get processing statistics for monitoring. */
  public EventProcessingStats getStats() {
    return EventProcessingStats.builder()
        .processedEvents(processedEvents.get())
        .failedEvents(failedEvents.get())
        .build();
  }

  /** Statistics for event processing. */
  public static class EventProcessingStats {
    private final long processedEvents;
    private final long failedEvents;

    public EventProcessingStats(long processedEvents, long failedEvents) {
      this.processedEvents = processedEvents;
      this.failedEvents = failedEvents;
    }

    public long getProcessedEvents() {
      return processedEvents;
    }

    public long getFailedEvents() {
      return failedEvents;
    }

    public double getFailureRate() {
      long total = processedEvents + failedEvents;
      return total > 0 ? (double) failedEvents / total : 0.0;
    }

    public static EventProcessingStatsBuilder builder() {
      return new EventProcessingStatsBuilder();
    }

    public static class EventProcessingStatsBuilder {
      private long processedEvents;
      private long failedEvents;

      public EventProcessingStatsBuilder processedEvents(long processedEvents) {
        this.processedEvents = processedEvents;
        return this;
      }

      public EventProcessingStatsBuilder failedEvents(long failedEvents) {
        this.failedEvents = failedEvents;
        return this;
      }

      public EventProcessingStats build() {
        return new EventProcessingStats(processedEvents, failedEvents);
      }
    }
  }
}
