package com.pacific.core.messaging.cqrs.query.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.pacific.core.messaging.cqrs.query.Query;
import com.pacific.core.messaging.cqrs.query.QueryBus;
import com.pacific.core.messaging.cqrs.query.QueryHandler;
import com.pacific.core.messaging.cqrs.query.QueryResult;

/** Simple implementation of QueryBus. Queries are typically handled locally, not via Kafka. */
@Slf4j
@Component
public class SimpleQueryBus implements QueryBus {

  private final Map<Class<?>, QueryHandler<?, ?>> handlers = new ConcurrentHashMap<>();

  @Override
  public <Q extends Query<R>, R> QueryResult<R> execute(Q query) {
    log.debug("Executing query: {} (cache key: {})", query.getQueryType(), query.getCacheKey());

    long startTime = System.currentTimeMillis();

    try {
      // Get handler
      QueryHandler<Q, R> handler = getHandler(query);

      // Execute query
      QueryResult<R> result = handler.handle(query);

      long duration = System.currentTimeMillis() - startTime;
      log.debug(
          "Query {} executed in {}ms (has data: {})",
          query.getQueryType(),
          duration,
          result.isPresent());

      return result;

    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error(
          "Failed to execute query {} after {}ms: {}",
          query.getQueryType(),
          duration,
          e.getMessage(),
          e);
      return QueryResult.empty();
    }
  }

  @Override
  public <Q extends Query<R>, R> CompletableFuture<QueryResult<R>> executeAsync(Q query) {
    return CompletableFuture.supplyAsync(() -> execute(query));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <Q extends Query<R>, R> void registerHandler(
      Class<Q> queryClass, QueryHandler<Q, R> handler) {

    if (handlers.containsKey(queryClass)) {
      throw new IllegalStateException(
          "Handler already registered for query: " + queryClass.getSimpleName());
    }

    handlers.put(queryClass, handler);
    log.info("Registered query handler for: {}", queryClass.getSimpleName());
  }

  /** Get handler for query. */
  @SuppressWarnings("unchecked")
  private <Q extends Query<R>, R> QueryHandler<Q, R> getHandler(Q query) {
    QueryHandler<Q, R> handler = (QueryHandler<Q, R>) handlers.get(query.getClass());

    if (handler == null) {
      throw new IllegalStateException(
          "No handler registered for query: " + query.getClass().getSimpleName());
    }

    return handler;
  }
}
