package com.pacific.shared.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Login response DTO shared across microservices */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

  private String accessToken;
  private String refreshToken;
  private String tokenType;
  private Long expiresIn;
  private UserDto user;
  private LocalDateTime timestamp;
}
