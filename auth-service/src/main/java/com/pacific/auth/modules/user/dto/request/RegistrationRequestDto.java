package com.pacific.auth.modules.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequestDto {
  private String username;
  private String email;
  private String password;
  private String firstName;
  private String lastName;
  private String phoneNumber;
  private String address;
}
