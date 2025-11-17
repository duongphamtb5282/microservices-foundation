package com.pacific.shared.messaging.cqrs.query;

/** Common interface for Query pattern - shared across all microservices */
public interface Query<R> {

  /** Gets the query type */
  String getQueryType();

  /** Gets the correlation ID */
  String getCorrelationId();

  /** Validates the query parameters */
  default void validate() {
    // Default implementation - override in concrete classes
  }
}
