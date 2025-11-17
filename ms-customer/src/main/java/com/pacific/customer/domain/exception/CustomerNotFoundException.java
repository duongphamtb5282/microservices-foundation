package com.pacific.customer.domain.exception;

/** Exception thrown when a customer is not found */
public class CustomerNotFoundException extends RuntimeException {

  private final String customerId;

  public CustomerNotFoundException(String customerId) {
    super("Customer not found with ID: " + customerId);
    this.customerId = customerId;
  }

  public CustomerNotFoundException(String customerId, Throwable cause) {
    super("Customer not found with ID: " + customerId, cause);
    this.customerId = customerId;
  }

  public String getCustomerId() {
    return customerId;
  }
}
