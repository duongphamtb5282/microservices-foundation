package com.pacific.shared.exceptions;

/**
 * Exception for business logic violations. This should be used when business rules are violated.
 */
public class BusinessException extends UserException {

  private final String errorCode;

  public BusinessException(String message) {
    super(message);
    this.errorCode = "BUSINESS_ERROR";
  }

  public BusinessException(String message, String errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  public BusinessException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = "BUSINESS_ERROR";
  }

  public BusinessException(String message, String errorCode, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
