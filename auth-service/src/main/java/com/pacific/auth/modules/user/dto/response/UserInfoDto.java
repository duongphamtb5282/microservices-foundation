package com.pacific.auth.modules.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** User information DTO with comprehensive user data */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private String id;
  private String userName;
  private String email;
  private String firstName;
  private String lastName;
  private String phoneNumber;
  private String address;
  private Boolean isActive;

  // JsonFormat not needed - JavaTimeModule handles Instant serialization automatically
  private Instant createdAt;

  private Instant modifiedAt;

  private List<String> roles;

  /**
   * Get full name (first name + last name) - computed field for JSON serialization READ_ONLY
   * ensures this is serialized but not deserialized
   */
  @JsonProperty(value = "fullName", access = JsonProperty.Access.READ_ONLY)
  public String getFullName() {
    if (firstName != null && lastName != null) {
      return firstName + " " + lastName;
    } else if (firstName != null) {
      return firstName;
    } else if (lastName != null) {
      return lastName;
    } else {
      return userName;
    }
  }

  /**
   * Get display name (preferred name for display) - computed field for JSON serialization READ_ONLY
   * ensures this is serialized but not deserialized
   */
  @JsonProperty(value = "displayName", access = JsonProperty.Access.READ_ONLY)
  public String getDisplayName() {
    if (firstName != null && lastName != null) {
      return firstName + " " + lastName;
    } else if (firstName != null) {
      return firstName;
    } else {
      return userName;
    }
  }

  /**
   * Check if user has any roles - computed field for JSON serialization READ_ONLY ensures this is
   * serialized but not deserialized
   */
  @JsonProperty(value = "hasRole", access = JsonProperty.Access.READ_ONLY)
  public boolean getHasRole() {
    return roles != null && !roles.isEmpty();
  }
}
