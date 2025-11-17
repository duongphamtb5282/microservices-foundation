package com.pacific.shared.messaging.cqrs.command;

/** Common interface for Command pattern - shared across all microservices */
public interface Command<R> {

  /** Gets the command ID */
  String getCommandId();

  /** Gets the initiator of the command */
  String getInitiator();

  /** Gets the command type */
  String getCommandType();

  /** Gets the correlation ID */
  default String getCorrelationId() {
    return getCommandId();
  }
}
