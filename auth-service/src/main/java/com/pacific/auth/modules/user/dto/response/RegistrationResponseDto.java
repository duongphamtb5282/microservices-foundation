package com.pacific.auth.modules.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponseDto {
  private String username;
  private String email;
  private String message;

  public RegistrationResponseDto(String username, String email) {
    this.username = username;
    this.email = email;
    this.message = "Registration successful";
  }
}
