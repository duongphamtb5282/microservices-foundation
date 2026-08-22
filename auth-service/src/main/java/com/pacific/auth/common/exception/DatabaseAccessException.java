package com.pacific.auth.common.exception;

/** Exception thrown when a raw database operation fails. */
public class DatabaseAccessException extends RuntimeException {

  public DatabaseAccessException(String message) {
    super(message);
  }

  public DatabaseAccessException(String message, Throwable cause) {
    super(message, cause);
  }
}
