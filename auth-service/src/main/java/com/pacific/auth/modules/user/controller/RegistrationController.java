package com.pacific.auth.modules.user.controller;

import com.pacific.auth.modules.user.dto.request.RegistrationRequestDto;
import com.pacific.auth.modules.user.dto.response.RegistrationResponseDto;
import com.pacific.auth.modules.user.service.UserRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Registration", description = "User registration management")
public class RegistrationController {

  private final UserRegistrationService userRegistrationService;

  @Operation(summary = "Register new user", description = "Register a new user account")
  @PostMapping("/register")
  public ResponseEntity<RegistrationResponseDto> register(
      @RequestBody RegistrationRequestDto request) {
    log.info("🚀 Processing registration request for user: {}", request.getUsername());

    RegistrationResponseDto response = userRegistrationService.registerUser(request);
    log.info("✅ Registration successful for user: {}", request.getUsername());
    return ResponseEntity.ok(response);
  }
}
