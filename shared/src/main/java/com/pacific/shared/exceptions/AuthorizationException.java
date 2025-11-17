package com.pacific.shared.exceptions;

/** Exception for authorization failures. This should be used when user authorization fails. */
public class AuthorizationException extends UserException {

  private final String errorCode;

  public AuthorizationException(String message) {
    super(message);
    this.errorCode = "AUTHORIZATION_ERROR";
  }

  public AuthorizationException(String message, String errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  public AuthorizationException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = "AUTHORIZATION_ERROR";
  }

  public AuthorizationException(String message, String errorCode, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
