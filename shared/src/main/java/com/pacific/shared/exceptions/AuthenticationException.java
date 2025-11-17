package com.pacific.shared.exceptions;

/** Exception for authentication failures. This should be used when user authentication fails. */
public class AuthenticationException extends UserException {

  private final String errorCode;

  public AuthenticationException(String message) {
    super(message);
    this.errorCode = "AUTHENTICATION_ERROR";
  }

  public AuthenticationException(String message, String errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  public AuthenticationException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = "AUTHENTICATION_ERROR";
  }

  public AuthenticationException(String message, String errorCode, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
