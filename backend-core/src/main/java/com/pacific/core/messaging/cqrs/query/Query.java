package com.pacific.core.messaging.cqrs.query;

import java.io.Serializable;

/**
 * Base interface for all queries in the system. Queries represent intent to read state (read
 * operations).
 *
 * <p>Queries should be: - Read-only (no side effects) - Idempotent - Cacheable
 *
 * @param <R> The type of result returned by the query
 */
public interface Query<R> extends Serializable {

  /**
   * Query type/name for routing and logging. Should be unique and descriptive (e.g.,
   * "GET_USER_BY_ID", "SEARCH_ORDERS").
   *
   * @return Query type name
   */
  String getQueryType();

  /**
   * Correlation ID for distributed tracing.
   *
   * @return Correlation ID
   */
  String getCorrelationId();

  /**
   * Cache key for query result caching. Default implementation uses query type and hashCode.
   * Override for custom cache key generation.
   *
   * @return Cache key
   */
  default String getCacheKey() {
    return getQueryType() + ":" + hashCode();
  }
}
