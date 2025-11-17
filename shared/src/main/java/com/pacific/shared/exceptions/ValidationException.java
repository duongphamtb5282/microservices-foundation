package com.pacific.shared.exceptions;

import java.util.Map;

/** Exception for validation failures. This should be used when input validation fails. */
public class ValidationException extends UserException {

  private final Map<String, String> validationErrors;
  private final String errorCode;

  public ValidationException(String message) {
    super(message);
    this.validationErrors = null;
    this.errorCode = "VALIDATION_ERROR";
  }

  public ValidationException(String message, Map<String, String> validationErrors) {
    super(message);
    this.validationErrors = validationErrors;
    this.errorCode = "VALIDATION_ERROR";
  }

  public ValidationException(String message, String errorCode) {
    super(message);
    this.validationErrors = null;
    this.errorCode = errorCode;
  }

  public ValidationException(
      String message, Map<String, String> validationErrors, String errorCode) {
    super(message);
    this.validationErrors = validationErrors;
    this.errorCode = errorCode;
  }

  public Map<String, String> getValidationErrors() {
    return validationErrors;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
