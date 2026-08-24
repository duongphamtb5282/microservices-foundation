package com.pacific.auth.modules.authentication.service;

import com.pacific.auth.modules.authentication.client.dto.KeycloakTokenResponse;
import com.pacific.auth.modules.authentication.dto.request.AuthenticationRequestDto;
import com.pacific.auth.modules.authentication.dto.request.RefreshTokenRequestDto;
import com.pacific.auth.modules.authentication.dto.response.AuthenticationResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Authentication service — thin Keycloak proxy. Dual-mode auth was removed (S-06): Keycloak is the
 * only authentication provider, so login, refresh and logout forward to Keycloak's token endpoints.
 * There is no local user store and no custom JWT issuance anymore.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

  private final KeycloakService keycloakService;

  /** Login via Keycloak password grant. */
  public AuthenticationResponseDto authenticate(final AuthenticationRequestDto request) {
    log.info("🚀 Processing login for user: {}", request.username());
    KeycloakTokenResponse tokenResponse =
        keycloakService.login(request.username(), request.password());
    log.info("✅ Login successful for user: {}", request.username());
    return toResponseDto(tokenResponse, request.username());
  }

  /** Refresh the access token via Keycloak refresh grant. */
  public AuthenticationResponseDto refreshToken(final RefreshTokenRequestDto request) {
    log.info("🔄 Refreshing token");
    KeycloakTokenResponse tokenResponse = keycloakService.refreshToken(request.refreshToken());
    log.info("✅ Token refreshed successfully");
    // Keycloak's refresh response does not carry the username — keep it null rather than decoding
    // the access token here (thin proxy).
    return toResponseDto(tokenResponse, null);
  }

  /** Logout — revoke the Keycloak refresh token. */
  public void logout(final String refreshToken) {
    log.info("🚪 Logging out user");
    keycloakService.logout(refreshToken);
    log.info("✅ Logout successful");
  }

  private AuthenticationResponseDto toResponseDto(
      KeycloakTokenResponse tokens, String username) {
    return AuthenticationResponseDto.builder()
        .accessToken(tokens.getAccessToken())
        .refreshToken(tokens.getRefreshToken())
        .tokenType(tokens.getTokenType() != null ? tokens.getTokenType() : "Bearer")
        .expiresIn(tokens.getExpiresIn() == null ? null : tokens.getExpiresIn().longValue())
        .username(username)
        .build();
  }
}
