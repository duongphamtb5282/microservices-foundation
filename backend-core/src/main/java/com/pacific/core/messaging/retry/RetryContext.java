package com.pacific.core.messaging.retry;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

/**
 * Context holding retry state for a message. Tracks attempt count, timing, and error information.
 */
@Data
@Builder
public class RetryContext {

  /** Unique event identifier */
  private String eventId;

  /** Topic the message was consumed from */
  private String topic;

  /** Partition number */
  private int partition;

  /** Message offset */
  private long offset;

  /** Current attempt number (1-based) */
  @Builder.Default private int attemptNumber = 0;

  /** Timestamp of first attempt */
  private Instant startTime;

  /** Timestamp of most recent attempt */
  private Instant lastAttemptTime;

  /** Correlation ID for distributed tracing */
  private String correlationId;

  /** Last exception that occurred */
  private Throwable lastException;

  /** Last exception message */
  private String lastExceptionMessage;

  /** Original message payload (for DLQ) */
  private Object originalPayload;

  /** Increment attempt counter and update timestamps. */
  public void incrementAttempt() {
    this.attemptNumber++;
    this.lastAttemptTime = Instant.now();

    if (this.startTime == null) {
      this.startTime = this.lastAttemptTime;
    }
  }

  /**
   * Check if should retry based on policy.
   *
   * @param policy The retry policy
   * @return true if should retry, false otherwise
   */
  public boolean shouldRetry(RetryPolicy policy) {
    return attemptNumber < policy.getMaxAttempts();
  }

  /**
   * Record exception for this attempt.
   *
   * @param exception The exception that occurred
   */
  public void recordException(Throwable exception) {
    this.lastException = exception;
    this.lastExceptionMessage = exception.getMessage();
  }

  /**
   * Get the event ID.
   *
   * @return the event ID
   */
  public String getEventId() {
    return eventId;
  }

  /**
   * Get the attempt count.
   *
   * @return the attempt count
   */
  public int getAttemptCount() {
    return attemptNumber;
  }

  /**
   * Get the start time.
   *
   * @return the start time
   */
  public Instant getStartTime() {
    return startTime;
  }

  /**
   * Get the correlation ID.
   *
   * @return the correlation ID
   */
  public String getCorrelationId() {
    return correlationId;
  }

  /**
   * Get the message ID (alias for eventId for backward compatibility).
   *
   * @return the message ID
   */
  public String getMessageId() {
    return eventId;
  }

  /**
   * Get the first attempt time (alias for startTime for backward compatibility).
   *
   * @return the first attempt time
   */
  public Instant getFirstAttemptTime() {
    return startTime;
  }
}
