package com.pacific.core.messaging.cqrs.query;

import java.util.concurrent.CompletableFuture;

/**
 * Query Bus for dispatching queries to appropriate handlers.
 *
 * <p>Responsibilities: - Route queries to registered handlers - Support caching of query results -
 * Handle cross-cutting concerns (logging, metrics)
 */
public interface QueryBus {

  /**
   * Execute query synchronously.
   *
   * @param query The query to execute
   * @param <Q> Query type
   * @param <R> Result type
   * @return QueryResult with data or empty result
   * @throws IllegalStateException if no handler is registered
   */
  <Q extends Query<R>, R> QueryResult<R> execute(Q query);

  /**
   * Execute query asynchronously.
   *
   * @param query The query to execute
   * @param <Q> Query type
   * @param <R> Result type
   * @return CompletableFuture that completes with QueryResult
   */
  <Q extends Query<R>, R> CompletableFuture<QueryResult<R>> executeAsync(Q query);

  /**
   * Register a query handler for a specific query type.
   *
   * @param queryClass The query class
   * @param handler The handler for this query type
   * @param <Q> Query type
   * @param <R> Result type
   */
  <Q extends Query<R>, R> void registerHandler(Class<Q> queryClass, QueryHandler<Q, R> handler);
}
