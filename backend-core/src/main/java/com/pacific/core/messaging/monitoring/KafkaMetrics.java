package com.pacific.core.messaging.monitoring;

import java.time.Duration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.pacific.core.messaging.config.KafkaWrapperProperties;

/** Metrics collector for Kafka wrapper operations. Uses Micrometer for vendor-neutral metrics. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaMetrics {

  private final MeterRegistry meterRegistry;
  private final KafkaWrapperProperties properties;

  /**
   * Record command execution.
   *
   * @param commandType The command type
   * @param success Whether execution was successful
   * @param durationMs Execution duration in milliseconds
   */
  public void recordCommandExecution(String commandType, boolean success, long durationMs) {
    String prefix = properties.getMonitoring().getMetricsPrefix();

    Timer.builder(prefix + ".command.execution")
        .tag("type", commandType)
        .tag("success", String.valueOf(success))
        .description("Command execution time")
        .register(meterRegistry)
        .record(Duration.ofMillis(durationMs));

    Counter.builder(prefix + ".command.count")
        .tag("type", commandType)
        .tag("success", String.valueOf(success))
        .description("Command execution count")
        .register(meterRegistry)
        .increment();
  }

  /**
   * Record query execution.
   *
   * @param queryType The query type
   * @param fromCache Whether result was from cache
   * @param durationMs Execution duration in milliseconds
   */
  public void recordQueryExecution(String queryType, boolean fromCache, long durationMs) {
    String prefix = properties.getMonitoring().getMetricsPrefix();

    Timer.builder(prefix + ".query.execution")
        .tag("type", queryType)
        .tag("from_cache", String.valueOf(fromCache))
        .description("Query execution time")
        .register(meterRegistry)
        .record(Duration.ofMillis(durationMs));

    Counter.builder(prefix + ".query.count")
        .tag("type", queryType)
        .tag("from_cache", String.valueOf(fromCache))
        .description("Query execution count")
        .register(meterRegistry)
        .increment();
  }

  /**
   * Record retry attempt.
   *
   * @param topic The Kafka topic
   * @param attemptNumber The attempt number
   */
  public void recordRetryAttempt(String topic, int attemptNumber) {
    String prefix = properties.getMonitoring().getMetricsPrefix();

    Counter.builder(prefix + ".retry.attempts")
        .tag("topic", topic)
        .tag("attempt", String.valueOf(attemptNumber))
        .description("Retry attempts count")
        .register(meterRegistry)
        .increment();
  }

  /**
   * Record DLQ message.
   *
   * @param originalTopic The original topic before DLQ
   */
  public void recordDlqMessage(String originalTopic) {
    String prefix = properties.getMonitoring().getMetricsPrefix();

    Counter.builder(prefix + ".dlq.messages")
        .tag("original_topic", originalTopic)
        .description("Messages sent to DLQ")
        .register(meterRegistry)
        .increment();
  }

  /**
   * Record event published.
   *
   * @param eventType The event type
   */
  public void recordEventPublished(String eventType) {
    String prefix = properties.getMonitoring().getMetricsPrefix();

    Counter.builder(prefix + ".events.published")
        .tag("type", eventType)
        .description("Events published to Kafka")
        .register(meterRegistry)
        .increment();
  }

  /**
   * Record event consumed.
   *
   * @param eventType The event type
   * @param success Whether consumption was successful
   */
  public void recordEventConsumed(String eventType, boolean success) {
    String prefix = properties.getMonitoring().getMetricsPrefix();

    Counter.builder(prefix + ".events.consumed")
        .tag("type", eventType)
        .tag("success", String.valueOf(success))
        .description("Events consumed from Kafka")
        .register(meterRegistry)
        .increment();
  }

  /**
   * Increment events consumed counter.
   *
   * @param topic The Kafka topic
   * @param eventType The event type
   */
  public void incrementEventsConsumed(String topic, String eventType) {
    String prefix = properties.getMonitoring().getMetricsPrefix();

    Counter.builder(prefix + ".events.consumed")
        .tag("topic", topic)
        .tag("type", eventType)
        .description("Events consumed from Kafka")
        .register(meterRegistry)
        .increment();
  }

  /**
   * Record event processing time.
   *
   * @param topic The Kafka topic
   * @param eventType The event type
   * @param processingTimeMs Processing time in milliseconds
   */
  public void recordEventProcessingTime(String topic, String eventType, long processingTimeMs) {
    String prefix = properties.getMonitoring().getMetricsPrefix();

    Timer.builder(prefix + ".events.processing.time")
        .tag("topic", topic)
        .tag("type", eventType)
        .description("Event processing time")
        .register(meterRegistry)
        .record(Duration.ofMillis(processingTimeMs));
  }

  /**
   * Increment failed events counter.
   *
   * @param topic The Kafka topic
   * @param eventType The event type
   */
  public void incrementFailedEvents(String topic, String eventType) {
    String prefix = properties.getMonitoring().getMetricsPrefix();

    Counter.builder(prefix + ".events.failed")
        .tag("topic", topic)
        .tag("type", eventType)
        .description("Failed events")
        .register(meterRegistry)
        .increment();
  }
}
