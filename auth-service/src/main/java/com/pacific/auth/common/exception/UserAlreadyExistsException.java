package com.pacific.auth.common.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * Exception thrown when attempting to register a user that already exists. Used for duplicate email
 * or username scenarios.
 */
@Getter
public class UserAlreadyExistsException extends RuntimeException {

  private final Map<String, String> details;

  public UserAlreadyExistsException(String message) {
    super(message);
    this.details = new HashMap<>();
  }

  public UserAlreadyExistsException(String message, Map<String, String> details) {
    super(message);
    this.details = details != null ? details : new HashMap<>();
  }

  /** Create exception for duplicate email */
  public static UserAlreadyExistsException forEmail(String email) {
    Map<String, String> details = new HashMap<>();
    details.put("email", email);
    details.put("field", "email");
    return new UserAlreadyExistsException(
        String.format("User with email '%s' already exists", email), details);
  }

  /** Create exception for duplicate username */
  public static UserAlreadyExistsException forUsername(String username) {
    Map<String, String> details = new HashMap<>();
    details.put("username", username);
    details.put("field", "username");
    return new UserAlreadyExistsException(
        String.format("User with username '%s' already exists", username), details);
  }

  /** Create exception for both duplicate email and username */
  public static UserAlreadyExistsException forBoth(String email, String username) {
    Map<String, String> details = new HashMap<>();
    details.put("email", email);
    details.put("username", username);
    details.put("fields", "email,username");
    return new UserAlreadyExistsException(
        String.format("User with email '%s' or username '%s' already exists", email, username),
        details);
  }
}
