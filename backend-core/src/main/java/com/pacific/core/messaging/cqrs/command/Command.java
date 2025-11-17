package com.pacific.core.messaging.cqrs.command;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all commands in the system. Commands represent intent to change state (write
 * operations).
 *
 * @param <R> The type of result returned after command execution
 */
public interface Command<R> extends Serializable {

  /**
   * Unique command identifier. Default implementation generates a UUID.
   *
   * @return Unique command ID
   */
  default String getCommandId() {
    return UUID.randomUUID().toString();
  }

  /**
   * Timestamp when command was created.
   *
   * @return Command creation timestamp
   */
  default Instant getTimestamp() {
    return Instant.now();
  }

  /**
   * User or system that initiated this command. Used for authorization and audit trail.
   *
   * @return Initiator identifier
   */
  default String getInitiator() {
    return "SYSTEM";
  }

  /**
   * Command type/name for routing and logging. Should be unique and descriptive (e.g.,
   * "CREATE_USER", "UPDATE_ORDER").
   *
   * @return Command type name
   */
  String getCommandType();

  /**
   * Correlation ID for distributed tracing. Links related commands and events across services.
   *
   * @return Correlation ID
   */
  String getCorrelationId();

  /**
   * Validate command before execution. Throws exception if validation fails. Override in concrete
   * commands for specific validation logic.
   *
   * @throws IllegalArgumentException if validation fails
   */
  default void validate() {
    // Override in concrete commands for validation
  }
}
