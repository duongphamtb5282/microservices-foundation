package com.pacific.customer.application.dto;

import com.pacific.customer.domain.model.Customer;
import com.pacific.customer.domain.model.CustomerStatus;
import java.time.Instant;
import java.time.LocalDate;

/** Response DTO for customer data */
public record CustomerResponse(
    String id,
    String email,
    String firstName,
    String lastName,
    String phone,
    LocalDate dateOfBirth,
    String language,
    String timezone,
    boolean emailNotifications,
    boolean smsNotifications,
    boolean pushNotifications,
    CustomerStatus status,
    Instant createdAt,
    Instant updatedAt) {
  /** Factory method to create from domain Customer */
  public static CustomerResponse from(Customer customer) {
    return new CustomerResponse(
        customer.id(),
        customer.email(),
        customer.profile().firstName(),
        customer.profile().lastName(),
        customer.profile().phone(),
        customer.profile().dateOfBirth(),
        customer.preferences().language(),
        customer.preferences().timezone(),
        customer.preferences().notifications().emailEnabled(),
        customer.preferences().notifications().smsEnabled(),
        customer.preferences().notifications().pushEnabled(),
        customer.status(),
        customer.createdAt(),
        customer.updatedAt());
  }
}
