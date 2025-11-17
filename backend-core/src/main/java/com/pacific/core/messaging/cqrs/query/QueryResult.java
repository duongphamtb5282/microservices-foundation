package com.pacific.core.messaging.cqrs.query;

import java.util.Optional;

import lombok.Value;

/**
 * Result wrapper for query execution. Always successful (queries don't fail, they just return empty
 * results).
 *
 * @param <T> The type of data returned
 */
@Value
public class QueryResult<T> {

  T data;
  boolean fromCache;

  /**
   * Create a query result with data.
   *
   * @param data The result data
   * @param <T> Data type
   * @return QueryResult
   */
  public static <T> QueryResult<T> of(T data) {
    return new QueryResult<>(data, false);
  }

  /**
   * Create an empty query result.
   *
   * @param <T> Data type
   * @return Empty QueryResult
   */
  public static <T> QueryResult<T> empty() {
    return new QueryResult<>(null, false);
  }

  /**
   * Create a query result from cache.
   *
   * @param data The cached data
   * @param <T> Data type
   * @return QueryResult marked as from cache
   */
  public static <T> QueryResult<T> fromCache(T data) {
    return new QueryResult<>(data, true);
  }

  /**
   * Get data as Optional.
   *
   * @return Optional containing data if present
   */
  public Optional<T> getData() {
    return Optional.ofNullable(data);
  }

  /**
   * Check if result is empty.
   *
   * @return true if no data, false otherwise
   */
  public boolean isEmpty() {
    return data == null;
  }

  /**
   * Check if result has data.
   *
   * @return true if data present, false otherwise
   */
  public boolean isPresent() {
    return data != null;
  }
}
