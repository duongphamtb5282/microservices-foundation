package com.pacific.core.messaging.error;

import lombok.Builder;
import lombok.Value;

/** Statistics for Dead Letter Queue monitoring. */
@Value
@Builder
public class DlqStats {

  /** Total number of messages in DLQ */
  long totalMessages;

  /** Rate of messages being added to DLQ (per second) */
  double retryRate;

  /** Number of messages reprocessed successfully */
  long reprocessedSuccessfully;

  /** Number of messages still failing after reprocess */
  long reprocessedFailed;
}
