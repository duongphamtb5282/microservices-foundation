package com.pacific.core.messaging.cqrs.query;

/** Thrown when a query cannot be dispatched: duplicate handler registration or missing handler. */
public class QueryBusException extends RuntimeException {

  public QueryBusException(String message) {
    super(message);
  }

  public QueryBusException(String message, Throwable cause) {
    super(message, cause);
  }
}
