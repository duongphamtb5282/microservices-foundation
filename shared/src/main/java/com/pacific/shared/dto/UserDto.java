package com.pacific.shared.dto;

import java.time.LocalDateTime;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** User DTO shared across microservices */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
  private String id;
  private String userName;
  private String email;
  private Set<String> roles;
  private LocalDateTime createdAt;
  private LocalDateTime modifiedAt;
  private String createdBy;
  private String modifiedBy;
}
