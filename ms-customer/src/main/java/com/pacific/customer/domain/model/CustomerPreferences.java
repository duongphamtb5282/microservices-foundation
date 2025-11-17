package com.pacific.customer.domain.model;

/**
 * Customer Preferences - Immutable Record Contains customer's notification and display preferences.
 */
public record CustomerPreferences(
    String language, String timezone, NotificationSettings notifications) {
  public CustomerPreferences {
    // Validation in compact constructor
    if (language == null || language.trim().isEmpty()) {
      throw new IllegalArgumentException("Language cannot be null or empty");
    }
    if (timezone == null || timezone.trim().isEmpty()) {
      throw new IllegalArgumentException("Timezone cannot be null or empty");
    }
    if (notifications == null) {
      throw new IllegalArgumentException("Notification settings cannot be null");
    }

    // Validate language format (ISO 639-1)
    if (!language.matches("^[a-z]{2,3}(-[A-Z]{2})?$")) {
      throw new IllegalArgumentException(
          "Invalid language format. Use ISO 639-1 (e.g., 'en', 'en-US')");
    }

    // Validate timezone
    if (!isValidTimezone(timezone)) {
      throw new IllegalArgumentException("Invalid timezone format");
    }
  }

  /** Default preferences for new customers */
  public static CustomerPreferences defaultPreferences() {
    return new CustomerPreferences("en", "UTC", NotificationSettings.defaultSettings());
  }

  /** Basic validation for timezone format */
  private static boolean isValidTimezone(String timezone) {
    // Simple validation - in production, you might want to use ZoneId.getAvailableZoneIds()
    return timezone.matches("^[A-Z][a-zA-Z/_+-]+(/[A-Z][a-zA-Z/_+-]+)*$")
        || timezone.equals("UTC")
        || timezone.matches("^UTC[+-]\\d{1,2}(:\\d{2})?$");
  }

  /** Notification Settings - Nested Record */
  public record NotificationSettings(
      boolean emailEnabled, boolean smsEnabled, boolean pushEnabled, boolean marketingEnabled) {
    public static NotificationSettings defaultSettings() {
      return new NotificationSettings(true, false, true, false);
    }

    /** Checks if any notification channel is enabled */
    public boolean hasAnyNotificationEnabled() {
      return emailEnabled || smsEnabled || pushEnabled;
    }

    /** Gets enabled notification types as a list */
    public java.util.List<String> getEnabledTypes() {
      java.util.List<String> types = new java.util.ArrayList<>();
      if (emailEnabled) types.add("email");
      if (smsEnabled) types.add("sms");
      if (pushEnabled) types.add("push");
      return types;
    }
  }
}
