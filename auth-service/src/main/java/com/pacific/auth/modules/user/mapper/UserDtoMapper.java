package com.pacific.auth.modules.user.mapper;

import com.pacific.auth.modules.role.entity.Role;
import com.pacific.auth.modules.role.entity.RoleType;
import com.pacific.auth.modules.user.dto.response.UserDTO;
import com.pacific.auth.modules.user.entity.User;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for converting between User entity and UserDTO. This mapper is specifically for
 * authentication purposes to avoid circular references.
 */
@Component
public class UserDtoMapper {

  public static final UserDtoMapper INSTANCE = new UserDtoMapper();

  /**
   * Convert User entity to UserDTO. This method extracts only the role names to avoid circular
   * references.
   */
  public UserDTO toDTO(User user) {
    if (user == null) {
      return null;
    }

    return UserDTO.builder()
        .id(user.getId())
        .userName(user.getUserName())
        .email(user.getEmail())
        .password(user.getPassword())
        .roleNames(toRoleTypes(user.getRoles()))
        .createdAt(mapDateTimeToInstant(user.getCreatedAt()))
        .createdBy(user.getCreatedBy())
        .modifiedAt(mapDateTimeToInstant(user.getModifiedAt()))
        .modifiedBy(user.getModifiedBy())
        .build();
  }

  /**
   * Convert UserDTO to User entity. This method creates a User entity with only the essential
   * fields.
   */
  public User toEntity(UserDTO userDTO) {
    if (userDTO == null) {
      return null;
    }

    return User.builder()
        .userName(userDTO.getUserName())
        .email(userDTO.getEmail())
        .password(userDTO.getPassword())
        .build();
  }

  /**
   * Maps Set of Role entities to Set of RoleType enums.
   *
   * @param roles the Set of Role entities
   * @return the Set of RoleType enums
   */
  public Set<RoleType> toRoleTypes(Set<Role> roles) {
    if (roles == null) {
      return null;
    }
    return roles.stream().map(Role::getName).collect(Collectors.toSet());
  }

  /**
   * Maps Instant to Instant (identity mapping).
   *
   * @param instant the Instant
   * @return the Instant
   */
  public java.time.Instant mapInstant(java.time.Instant instant) {
    return instant;
  }

  /**
   * Maps LocalDateTime to Instant.
   *
   * @param localDateTime the LocalDateTime
   * @return the Instant
   */
  public java.time.Instant mapLocalDateTimeToInstant(java.time.LocalDateTime localDateTime) {
    if (localDateTime == null) {
      return null;
    }
    return localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
  }

  /**
   * Maps Object (LocalDateTime or Instant) to Instant.
   *
   * @param dateTime the date time object
   * @return the Instant
   */
  public java.time.Instant mapDateTimeToInstant(Object dateTime) {
    if (dateTime == null) {
      return null;
    }
    if (dateTime instanceof java.time.Instant) {
      return (java.time.Instant) dateTime;
    }
    if (dateTime instanceof java.time.LocalDateTime) {
      return ((java.time.LocalDateTime) dateTime)
          .atZone(java.time.ZoneId.systemDefault())
          .toInstant();
    }
    return null;
  }
}
