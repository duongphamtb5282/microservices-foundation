package com.pacific.core.messaging.cqrs.query;

/**
 * Thrown when a query fails to execute. Propagated to the caller so failures surface as errors
 * (e.g. HTTP 500 via the global advice) instead of masquerading as an empty result (6b).
 */
public class QueryExecutionException extends RuntimeException {

  public QueryExecutionException(String message, Throwable cause) {
    super(message, cause);
  }
}
