package com.pacific.customer.infrastructure.persistence.document;

import com.pacific.customer.domain.model.Customer;
import com.pacific.customer.domain.model.CustomerPreferences;
import com.pacific.customer.domain.model.CustomerProfile;
import com.pacific.customer.domain.model.CustomerStatus;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document representation of Customer domain model. Uses mutable fields for MongoDB
 * compatibility while maintaining domain integrity.
 */
@Document(collection = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDocument {

  @Id private String id;

  @Indexed(unique = true)
  private String email;

  private CustomerProfileDocument profile;
  private CustomerPreferencesDocument preferences;
  private CustomerStatus status;

  @CreatedDate private Instant createdAt;

  @LastModifiedDate private Instant updatedAt;

  @Version private Long version;

  /** Convert domain Customer to CustomerDocument */
  public static CustomerDocument fromDomain(Customer customer) {
    return CustomerDocument.builder()
        .id(customer.id())
        .email(customer.email())
        .profile(CustomerProfileDocument.fromDomain(customer.profile()))
        .preferences(CustomerPreferencesDocument.fromDomain(customer.preferences()))
        .status(customer.status())
        .createdAt(customer.createdAt())
        .updatedAt(customer.updatedAt())
        .version(customer.version())
        .build();
  }

  /** Convert CustomerDocument to domain Customer */
  public Customer toDomain() {
    return new Customer(
        id,
        email,
        profile.toDomain(),
        preferences.toDomain(),
        status,
        createdAt,
        updatedAt,
        version);
  }

  /** Customer Profile Document */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CustomerProfileDocument {
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate dateOfBirth;

    public static CustomerProfileDocument fromDomain(CustomerProfile profile) {
      return CustomerProfileDocument.builder()
          .firstName(profile.firstName())
          .lastName(profile.lastName())
          .phone(profile.phone())
          .dateOfBirth(profile.dateOfBirth())
          .build();
    }

    public CustomerProfile toDomain() {
      return new CustomerProfile(firstName, lastName, phone, dateOfBirth);
    }
  }

  /** Customer Preferences Document */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CustomerPreferencesDocument {
    private String language;
    private String timezone;
    private NotificationSettingsDocument notifications;

    public static CustomerPreferencesDocument fromDomain(CustomerPreferences preferences) {
      return CustomerPreferencesDocument.builder()
          .language(preferences.language())
          .timezone(preferences.timezone())
          .notifications(NotificationSettingsDocument.fromDomain(preferences.notifications()))
          .build();
    }

    public CustomerPreferences toDomain() {
      return new CustomerPreferences(language, timezone, notifications.toDomain());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationSettingsDocument {
      private boolean emailEnabled;
      private boolean smsEnabled;
      private boolean pushEnabled;
      private boolean marketingEnabled;

      public static NotificationSettingsDocument fromDomain(
          CustomerPreferences.NotificationSettings settings) {
        return NotificationSettingsDocument.builder()
            .emailEnabled(settings.emailEnabled())
            .smsEnabled(settings.smsEnabled())
            .pushEnabled(settings.pushEnabled())
            .marketingEnabled(settings.marketingEnabled())
            .build();
      }

      public CustomerPreferences.NotificationSettings toDomain() {
        return new CustomerPreferences.NotificationSettings(
            emailEnabled, smsEnabled, pushEnabled, marketingEnabled);
      }
    }
  }
}
