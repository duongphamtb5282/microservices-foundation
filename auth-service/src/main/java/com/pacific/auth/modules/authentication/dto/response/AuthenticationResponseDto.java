package com.pacific.auth.modules.authentication.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponseDto {
  private String accessToken;
  private String refreshToken;
  private String tokenType;
  private Long expiresIn;
  private String username;
}
