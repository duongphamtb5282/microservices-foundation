package com.pacific.customer.domain.exception;

/**
 * Exception thrown when a customer with the given email already exists (either detected by the
 * pre-insert check or surfaced by the unique email index on a concurrent insert race).
 */
public class CustomerAlreadyExistsException extends RuntimeException {

  private final String email;

  public CustomerAlreadyExistsException(String email) {
    super("Customer already exists with email: " + email);
    this.email = email;
  }

  public CustomerAlreadyExistsException(String email, Throwable cause) {
    super("Customer already exists with email: " + email, cause);
    this.email = email;
  }

  public String getEmail() {
    return email;
  }
}
