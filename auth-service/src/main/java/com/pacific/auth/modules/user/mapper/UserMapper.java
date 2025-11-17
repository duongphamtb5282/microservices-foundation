package com.pacific.auth.modules.user.mapper;

import com.pacific.auth.modules.role.entity.Role;
import com.pacific.auth.modules.role.entity.RoleType;
import com.pacific.auth.modules.user.dto.request.RegistrationRequestDto;
import com.pacific.auth.modules.user.dto.response.RegistrationResponseDto;
import com.pacific.auth.modules.user.dto.response.UserDTO;
import com.pacific.auth.modules.user.entity.User;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manual mapper for User entity and DTOs. Provides object mapping between User entity and various
 * DTOs. Using manual mapping to avoid MapStruct compilation issues.
 */
public class UserMapper {

  public static final UserMapper INSTANCE = new UserMapper();

  /**
   * Maps User entity to UserDTO.
   *
   * @param user the User entity
   * @return the UserDTO
   */
  public UserDTO toUserDTO(User user) {
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
   * Maps UserDTO to User entity.
   *
   * @param userDTO the UserDTO
   * @return the User entity
   */
  public User toUser(UserDTO userDTO) {
    if (userDTO == null) {
      return null;
    }

    return User.builder()
        .userName(userDTO.getUserName())
        .email(userDTO.getEmail())
        .password(userDTO.getPassword())
        .roles(toRoles(userDTO.getRoleNames()))
        .build();
  }

  /**
   * Maps RegistrationRequestDto to User entity.
   *
   * @param requestDto the RegistrationRequestDto
   * @return the User entity
   */
  public User toUser(RegistrationRequestDto requestDto) {
    if (requestDto == null) {
      return null;
    }

    return User.builder()
        .userName(requestDto.getUsername())
        .email(requestDto.getEmail())
        .password(requestDto.getPassword())
        .firstName(requestDto.getFirstName())
        .lastName(requestDto.getLastName())
        .phoneNumber(requestDto.getPhoneNumber())
        .address(requestDto.getAddress())
        .isActive(true) // New users are active by default
        .build();
  }

  /**
   * Maps User entity to RegistrationResponseDto.
   *
   * @param user the User entity
   * @return the RegistrationResponseDto
   */
  public RegistrationResponseDto toRegistrationResponseDto(User user) {
    if (user == null) {
      return null;
    }
    return RegistrationResponseDto.builder()
        .username(user.getUserName())
        .email(user.getEmail())
        .message("Registration successful")
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
   * Maps Set of RoleType enums to Set of Role entities.
   *
   * @param roleTypes the Set of RoleType enums
   * @return the Set of Role entities
   */
  public Set<Role> toRoles(Set<RoleType> roleTypes) {
    if (roleTypes == null) {
      return null;
    }
    return roleTypes.stream()
        .map(roleType -> new Role(roleType, "Auto-generated role"))
        .collect(Collectors.toSet());
  }

  /**
   * Maps Set of Role entities to Set of String role names.
   *
   * @param roles the Set of Role entities
   * @return the Set of String role names
   */
  public Set<String> toRoleNames(Set<Role> roles) {
    if (roles == null) {
      return null;
    }
    return roles.stream().map(role -> role.getName().name()).collect(Collectors.toSet());
  }

  /**
   * Maps Instant to LocalDateTime.
   *
   * @param instant the Instant
   * @return the LocalDateTime
   */
  public LocalDateTime toLocalDateTime(Instant instant) {
    if (instant == null) {
      return null;
    }
    return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
  }

  /**
   * Maps Instant to Instant (identity mapping).
   *
   * @param instant the Instant
   * @return the Instant
   */
  public Instant mapInstant(Instant instant) {
    return instant;
  }

  /**
   * Maps LocalDateTime to Instant.
   *
   * @param localDateTime the LocalDateTime
   * @return the Instant
   */
  public Instant mapLocalDateTimeToInstant(LocalDateTime localDateTime) {
    if (localDateTime == null) {
      return null;
    }
    return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
  }

  /**
   * Maps Object (LocalDateTime or Instant) to Instant.
   *
   * @param dateTime the date time object
   * @return the Instant
   */
  public Instant mapDateTimeToInstant(Object dateTime) {
    if (dateTime == null) {
      return null;
    }
    if (dateTime instanceof Instant) {
      return (Instant) dateTime;
    }
    if (dateTime instanceof LocalDateTime) {
      return ((LocalDateTime) dateTime).atZone(ZoneId.systemDefault()).toInstant();
    }
    return null;
  }
}
