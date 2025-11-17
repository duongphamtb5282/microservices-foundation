package com.pacific.customer.application.dto;

import com.pacific.customer.domain.model.CustomerPreferences;
import java.time.LocalDate;
import java.util.Objects;

/** Request DTO for creating a new customer */
public record CreateCustomerRequest(
    String email,
    String firstName,
    String lastName,
    String phone,
    LocalDate dateOfBirth,
    String language,
    String timezone,
    boolean emailNotifications,
    boolean smsNotifications,
    boolean pushNotifications) {
  public CreateCustomerRequest {
    Objects.requireNonNull(email, "Email cannot be null");
    Objects.requireNonNull(firstName, "First name cannot be null");
    Objects.requireNonNull(lastName, "Last name cannot be null");

    if (email.trim().isEmpty()) {
      throw new IllegalArgumentException("Email cannot be empty");
    }
    if (firstName.trim().isEmpty()) {
      throw new IllegalArgumentException("First name cannot be empty");
    }
    if (lastName.trim().isEmpty()) {
      throw new IllegalArgumentException("Last name cannot be empty");
    }
    if (!email.contains("@")) {
      throw new IllegalArgumentException("Invalid email format");
    }
    if (language != null && language.trim().isEmpty()) {
      throw new IllegalArgumentException("Language cannot be empty if provided");
    }
    if (timezone != null && timezone.trim().isEmpty()) {
      throw new IllegalArgumentException("Timezone cannot be empty if provided");
    }
  }

  /** Gets the customer preferences from this request */
  public CustomerPreferences getCustomerPreferences() {
    String lang = language != null ? language : "en";
    String tz = timezone != null ? timezone : "UTC";

    CustomerPreferences.NotificationSettings notifications =
        new CustomerPreferences.NotificationSettings(
            emailNotifications,
            smsNotifications,
            pushNotifications,
            false // marketing disabled by default
            );

    return new CustomerPreferences(lang, tz, notifications);
  }

  /** Factory method with default values */
  public static CreateCustomerRequest withDefaults(
      String email, String firstName, String lastName) {
    return new CreateCustomerRequest(
        email, firstName, lastName, null, null, "en", "UTC", true, false, true);
  }
}
