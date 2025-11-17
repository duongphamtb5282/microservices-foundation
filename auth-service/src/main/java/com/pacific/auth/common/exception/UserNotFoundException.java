package com.pacific.auth.common.exception;

/** Exception thrown when a user is not found in the system. */
public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException(String message) {
    super(message);
  }

  public UserNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  /** Create exception for user not found by ID */
  public static UserNotFoundException forId(String userId) {
    return new UserNotFoundException(String.format("User not found with ID: %s", userId));
  }

  /** Create exception for user not found by username */
  public static UserNotFoundException forUsername(String username) {
    return new UserNotFoundException(String.format("User not found with username: %s", username));
  }

  /** Create exception for user not found by email */
  public static UserNotFoundException forEmail(String email) {
    return new UserNotFoundException(String.format("User not found with email: %s", email));
  }
}
