package com.pacific.core.messaging.cqrs.query;

/**
 * Handler for processing queries. Each query type should have exactly ONE handler.
 *
 * <p>Query handlers should: - Be read-only (no side effects) - Be idempotent - Return DTOs, not
 * domain entities - Be optimized for read performance
 *
 * @param <Q> The query type this handler processes
 * @param <R> The result type returned
 */
@FunctionalInterface
public interface QueryHandler<Q extends Query<R>, R> {

  /**
   * Handle the query and return result.
   *
   * <p>This method should: 1. Fetch data from read model/database 2. Transform to appropriate DTO
   * 3. Return result (may be cached)
   *
   * @param query The query to handle
   * @return QueryResult with data or empty result
   */
  QueryResult<R> handle(Q query);
}
