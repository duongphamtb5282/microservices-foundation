package com.pacific.core.messaging.error;

import java.util.Optional;

import com.pacific.core.messaging.retry.RetryContext;

/**
 * Manages failed messages that couldn't be processed after retries. Provides storage, retrieval,
 * and reprocessing capabilities.
 */
public interface DeadLetterQueue {

  /**
   * Send failed message to DLQ.
   *
   * @param context The retry context with message details
   * @param exception The exception that caused final failure
   */
  void send(RetryContext context, Throwable exception);

  /**
   * Retrieve message from DLQ for inspection.
   *
   * @param messageId The message ID to retrieve
   * @return Optional containing DLQ message if found
   */
  Optional<DlqMessage> retrieve(String messageId);

  /**
   * Reprocess message from DLQ.
   *
   * @param messageId The message ID to reprocess
   */
  void reprocess(String messageId);

  /**
   * Get DLQ statistics.
   *
   * @return DLQ statistics
   */
  DlqStats getStats();
}
