package com.pacific.customer.domain.exception;

/** Exception thrown when a customer operation fails */
public class CustomerOperationException extends RuntimeException {

  private final String operation;
  private final String customerId;

  public CustomerOperationException(String operation, String customerId, String message) {
    super(message);
    this.operation = operation;
    this.customerId = customerId;
  }

  public CustomerOperationException(
      String operation, String customerId, String message, Throwable cause) {
    super(message, cause);
    this.operation = operation;
    this.customerId = customerId;
  }

  public String getOperation() {
    return operation;
  }

  public String getCustomerId() {
    return customerId;
  }
}
