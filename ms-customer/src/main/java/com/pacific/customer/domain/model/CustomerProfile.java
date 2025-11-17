package com.pacific.customer.domain.model;

import java.time.LocalDate;

/** Customer Profile - Immutable Record Contains personal information about the customer. */
public record CustomerProfile(
    String firstName, String lastName, String phone, LocalDate dateOfBirth) {
  public CustomerProfile {
    // Validation in compact constructor
    if (firstName == null || firstName.trim().isEmpty()) {
      throw new IllegalArgumentException("First name cannot be null or empty");
    }
    if (lastName == null || lastName.trim().isEmpty()) {
      throw new IllegalArgumentException("Last name cannot be null or empty");
    }
    if (firstName.length() > 50) {
      throw new IllegalArgumentException("First name cannot exceed 50 characters");
    }
    if (lastName.length() > 50) {
      throw new IllegalArgumentException("Last name cannot exceed 50 characters");
    }
    if (phone != null && phone.length() > 20) {
      throw new IllegalArgumentException("Phone number cannot exceed 20 characters");
    }
    if (dateOfBirth != null && dateOfBirth.isAfter(LocalDate.now())) {
      throw new IllegalArgumentException("Date of birth cannot be in the future");
    }
  }

  /** Gets the full name */
  public String getFullName() {
    return firstName + " " + lastName;
  }

  /** Checks if the customer is an adult (18+ years old) */
  public boolean isAdult() {
    if (dateOfBirth == null) return true; // Assume adult if DOB not provided
    return dateOfBirth.isBefore(LocalDate.now().minusYears(18));
  }

  /** Gets the customer's age */
  public int getAge() {
    if (dateOfBirth == null) return 0;
    return LocalDate.now().getYear() - dateOfBirth.getYear();
  }
}
