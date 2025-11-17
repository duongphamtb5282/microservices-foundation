package com.pacific.customer.domain.model;

/** Customer Status Enum Represents the lifecycle states of a customer account. */
public enum CustomerStatus {
  ACTIVE("Active customer account"),
  INACTIVE("Temporarily inactive account"),
  SUSPENDED("Account suspended due to policy violation"),
  CLOSED("Account permanently closed"),
  PENDING_VERIFICATION("Account awaiting email/phone verification");

  private final String description;

  CustomerStatus(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  /** Checks if the status allows normal operations */
  public boolean isOperational() {
    return this == ACTIVE;
  }

  /** Checks if the status allows login */
  public boolean allowsLogin() {
    return this == ACTIVE || this == PENDING_VERIFICATION;
  }

  /** Checks if the status is terminal (cannot be changed) */
  public boolean isTerminal() {
    return this == CLOSED;
  }

  /** Gets all active statuses (for queries) */
  public static java.util.Set<CustomerStatus> getActiveStatuses() {
    return java.util.Set.of(ACTIVE, INACTIVE, PENDING_VERIFICATION);
  }

  /** Gets all inactive statuses */
  public static java.util.Set<CustomerStatus> getInactiveStatuses() {
    return java.util.Set.of(SUSPENDED, CLOSED);
  }
}
