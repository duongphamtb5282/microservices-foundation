package com.pacific.auth.modules.authentication.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pacific.auth.modules.authentication.security.jwt.common.JwtValidationResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class UserInfoMapperTest {

  private final UserInfoMapper mapper = new UserInfoMapper();

  @Test
  void toKeycloakUserInfo_mapsAllClaims() {
    Instant issuedAt = Instant.parse("2026-08-24T00:00:00Z");
    Instant expiresAt = Instant.parse("2026-08-24T01:00:00Z");
    JwtValidationResult validation =
        JwtValidationResult.builder()
            .valid(true)
            .username("john.doe")
            .email("john@example.com")
            .firstName("John")
            .lastName("Doe")
            .roles(List.of("USER", "ADMIN"))
            .issuer("https://keycloak/realms/demo")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build();

    Map<String, Object> result = mapper.toKeycloakUserInfo(validation, "KEYCLOAK");

    assertThat(result)
        .containsEntry("username", "john.doe")
        .containsEntry("email", "john@example.com")
        .containsEntry("firstName", "John")
        .containsEntry("lastName", "Doe")
        .containsEntry("roles", List.of("USER", "ADMIN"))
        .containsEntry("issuer", "https://keycloak/realms/demo")
        .containsEntry("issuedAt", issuedAt)
        .containsEntry("expiresAt", expiresAt)
        .containsEntry("tokenType", "KEYCLOAK");
  }

  @Test
  void toCustomJwtUserInfo_mapsAuthentication() {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            "custom.user",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")));

    Map<String, Object> result = mapper.toCustomJwtUserInfo(authentication, "CUSTOM");

    assertThat(result)
        .containsEntry("username", "custom.user")
        .containsEntry("authorities", List.of("ROLE_USER", "ROLE_ADMIN"))
        .containsEntry("tokenType", "CUSTOM")
        .containsEntry("isCustomJwt", true)
        .containsEntry("isKeycloakJwt", false);
  }

  @Test
  void toTokenValidationResponse_validIncludesClaims() {
    JwtValidationResult validation =
        JwtValidationResult.success(
            "jane",
            "jane@example.com",
            "Jane",
            "Smith",
            List.of("USER"),
            "issuer",
            Instant.parse("2026-08-24T00:00:00Z"),
            Instant.parse("2026-08-24T01:00:00Z"),
            Map.of(),
            "KEYCLOAK");

    Map<String, Object> result = mapper.toTokenValidationResponse(validation);

    assertThat(result).containsEntry("valid", true).containsEntry("username", "jane");
    assertThat(result).doesNotContainKey("error");
  }

  @Test
  void toTokenValidationResponse_invalidIncludesError() {
    JwtValidationResult validation = JwtValidationResult.failure("Token has expired");

    Map<String, Object> result = mapper.toTokenValidationResponse(validation);

    assertThat(result).containsEntry("valid", false).containsEntry("error", "Token has expired");
    assertThat(result).doesNotContainKey("username");
  }

  @Test
  void toKeycloakUserInfo_nullRolesStillMaps() {
    JwtValidationResult validation =
        JwtValidationResult.builder().valid(true).username("u").roles(null).build();

    Map<String, Object> result = mapper.toKeycloakUserInfo(validation, "KEYCLOAK");

    assertThat(result).containsEntry("roles", null).containsEntry("username", "u");
  }
}
