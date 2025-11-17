package com.pacific.core.messaging.cqrs.command;

import java.util.Optional;

import lombok.Value;

/**
 * Result wrapper for command execution. Encapsulates success/failure state with data or error
 * information.
 *
 * @param <T> The type of data returned on success
 */
@Value
public class CommandResult<T> {

  boolean success;
  T data;
  String errorMessage;
  String errorCode;

  /**
   * Create a successful result with data.
   *
   * @param data The result data
   * @param <T> Data type
   * @return Success CommandResult
   */
  public static <T> CommandResult<T> success(T data) {
    return new CommandResult<>(true, data, null, null);
  }

  /**
   * Create a successful result without data.
   *
   * @param <T> Data type
   * @return Success CommandResult with null data
   */
  public static <T> CommandResult<T> success() {
    return new CommandResult<>(true, null, null, null);
  }

  /**
   * Create a failure result with error message.
   *
   * @param errorMessage The error message
   * @param <T> Data type
   * @return Failure CommandResult
   */
  public static <T> CommandResult<T> failure(String errorMessage) {
    return new CommandResult<>(false, null, errorMessage, null);
  }

  /**
   * Create a failure result with error message and code.
   *
   * @param errorMessage The error message
   * @param errorCode The error code for programmatic handling
   * @param <T> Data type
   * @return Failure CommandResult
   */
  public static <T> CommandResult<T> failure(String errorMessage, String errorCode) {
    return new CommandResult<>(false, null, errorMessage, errorCode);
  }

  /**
   * Get data as Optional.
   *
   * @return Optional containing data if present
   */
  public Optional<T> getDataOptional() {
    return Optional.ofNullable(data);
  }

  /**
   * Check if result is a failure.
   *
   * @return true if failure, false if success
   */
  public boolean isFailure() {
    return !success;
  }
}
