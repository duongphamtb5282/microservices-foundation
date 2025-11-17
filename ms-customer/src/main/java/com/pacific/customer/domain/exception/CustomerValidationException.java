package com.pacific.customer.domain.exception;

/** Exception thrown when customer validation fails */
public class CustomerValidationException extends RuntimeException {

  private final String field;
  private final String value;

  public CustomerValidationException(String message) {
    super(message);
    this.field = null;
    this.value = null;
  }

  public CustomerValidationException(String field, String value, String message) {
    super(message);
    this.field = field;
    this.value = value;
  }

  public CustomerValidationException(String message, Throwable cause) {
    super(message, cause);
    this.field = null;
    this.value = null;
  }

  public String getField() {
    return field;
  }

  public String getValue() {
    return value;
  }
}
