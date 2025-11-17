package com.pacific.shared.exceptions;

import java.util.Map;

/** Utility class for creating common exceptions with consistent error codes and messages. */
public class ExceptionUtils {

  // Common error codes
  public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
  public static final String USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
  public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
  public static final String INVALID_TOKEN = "INVALID_TOKEN";
  public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
  public static final String INSUFFICIENT_PERMISSIONS = "INSUFFICIENT_PERMISSIONS";
  public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
  public static final String RESOURCE_CONFLICT = "RESOURCE_CONFLICT";
  public static final String OPERATION_NOT_ALLOWED = "OPERATION_NOT_ALLOWED";

  /** Create a user not found exception */
  public static ResourceNotFoundException userNotFound(String username) {
    return new ResourceNotFoundException("User with username '" + username + "' not found");
  }

  /** Create a user not found exception with custom message */
  public static ResourceNotFoundException userNotFound(String message, String username) {
    return new ResourceNotFoundException(message + ": " + username);
  }

  /** Create a user already exists exception */
  public static BusinessException userAlreadyExists(String username) {
    return new BusinessException(
        "User with username '" + username + "' already exists", USER_ALREADY_EXISTS);
  }

  /** Create an invalid credentials exception */
  public static AuthenticationException invalidCredentials() {
    return new AuthenticationException("Invalid username or password", INVALID_CREDENTIALS);
  }

  /** Create an invalid credentials exception with custom message */
  public static AuthenticationException invalidCredentials(String message) {
    return new AuthenticationException(message, INVALID_CREDENTIALS);
  }

  /** Create an invalid token exception */
  public static AuthenticationException invalidToken() {
    return new AuthenticationException("Invalid or malformed token", INVALID_TOKEN);
  }

  /** Create an invalid token exception with custom message */
  public static AuthenticationException invalidToken(String message) {
    return new AuthenticationException(message, INVALID_TOKEN);
  }

  /** Create a token expired exception */
  public static AuthenticationException tokenExpired() {
    return new AuthenticationException("Token has expired", TOKEN_EXPIRED);
  }

  /** Create an insufficient permissions exception */
  public static AuthorizationException insufficientPermissions() {
    return new AuthorizationException(
        "Insufficient permissions to perform this action", INSUFFICIENT_PERMISSIONS);
  }

  /** Create an insufficient permissions exception with custom message */
  public static AuthorizationException insufficientPermissions(String message) {
    return new AuthorizationException(message, INSUFFICIENT_PERMISSIONS);
  }

  /** Create a validation exception with field errors */
  public static ValidationException validationFailed(
      String message, Map<String, String> fieldErrors) {
    return new ValidationException(message, fieldErrors, VALIDATION_FAILED);
  }

  /** Create a validation exception with single field error */
  public static ValidationException validationFailed(String message, String field, String error) {
    return new ValidationException(message, Map.of(field, error), VALIDATION_FAILED);
  }

  /** Create a resource conflict exception */
  public static ResourceConstraintException resourceConflict(String message) {
    return new ResourceConstraintException(message);
  }

  /** Create an operation not allowed exception */
  public static BusinessException operationNotAllowed(String message) {
    return new BusinessException(message, OPERATION_NOT_ALLOWED);
  }

  /** Create a business rule violation exception */
  public static BusinessException businessRuleViolation(String message) {
    return new BusinessException(message, "BUSINESS_RULE_VIOLATION");
  }

  /** Create a business rule violation exception with error code */
  public static BusinessException businessRuleViolation(String message, String errorCode) {
    return new BusinessException(message, errorCode);
  }
}
