package com.pacific.core.messaging.error;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

/** Message stored in Dead Letter Queue with failure context. */
@Value
@Builder
public class DlqMessage {

  /** Unique message identifier */
  String messageId;

  /** Original topic message was consumed from */
  String originalTopic;

  /** Partition number */
  int partition;

  /** Message offset */
  long offset;

  /** Number of retry attempts made */
  int attemptNumber;

  /** Timestamp of first attempt */
  Instant firstAttemptTime;

  /** Timestamp of last attempt */
  Instant lastAttemptTime;

  /** Exception class name */
  String exceptionClass;

  /** Exception message */
  String exceptionMessage;

  /** Full stack trace */
  String stackTrace;

  /** Original message payload */
  Object originalPayload;

  /** Timestamp when sent to DLQ */
  Instant timestamp;
}
