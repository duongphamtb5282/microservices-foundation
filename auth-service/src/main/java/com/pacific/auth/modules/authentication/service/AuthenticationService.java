package com.pacific.auth.modules.authentication.service;

import com.pacific.auth.modules.authentication.dto.request.AuthenticationRequestDto;
import com.pacific.auth.modules.authentication.dto.request.RefreshTokenRequestDto;
import com.pacific.auth.modules.authentication.dto.response.AuthenticationResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  @Autowired(required = false)
  private StatelessRefreshTokenService statelessRefreshTokenService;

  @Transactional
  public AuthenticationResponseDto authenticate(final AuthenticationRequestDto request) {
    log.info("🚀 Starting authentication for user: {}", request.username());

    try {
      final var authToken =
          UsernamePasswordAuthenticationToken.unauthenticated(
              request.username(), request.password());
      log.info("🔐 Created authentication token for user: {}", request.username());

      final var authentication = authenticationManager.authenticate(authToken);
      log.info("✅ Authentication successful for user: {}", request.username());

      // Load user details with roles for JWT token generation
      log.info("👤 Loading user details for JWT token generation: {}", request.username());
      final UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
      log.info("✅ User details loaded with {} authorities", userDetails.getAuthorities().size());

      // Generate access token with user roles
      log.info("🔑 Generating access token with roles for user: {}", request.username());
      final var accessToken = jwtService.generateAccessToken(userDetails);
      log.info("✅ Access token with roles generated successfully for user: {}", request.username());

      // Create stateless refresh token
      log.info("🔄 Generating refresh token for user: {}", request.username());
      final String refreshToken;
      if (statelessRefreshTokenService != null) {
        refreshToken =
            statelessRefreshTokenService.generateStatelessRefreshToken(request.username());
        log.info("✅ Refresh token generated successfully for user: {}", request.username());
      } else {
        // Fallback: generate a simple refresh token using JwtService
        refreshToken = jwtService.generateRefreshToken(request.username());
        log.info(
            "✅ Fallback refresh token generated successfully for user: {}", request.username());
      }

      log.info("🎉 Authentication response created successfully for user: {}", request.username());
      return AuthenticationResponseDto.builder()
          .accessToken(accessToken)
          .refreshToken(refreshToken)
          .tokenType("Bearer")
          .username(request.username())
          .build();

    } catch (Exception e) {
      log.error(
          "❌ Authentication failed for user: {} - Error: {}",
          request.username(),
          e.getMessage(),
          e);
      throw e;
    }
  }

  @Transactional
  public AuthenticationResponseDto refreshToken(final RefreshTokenRequestDto request) {
    if (statelessRefreshTokenService != null) {
      // Use stateless refresh token service
      return statelessRefreshTokenService.refreshToken(request);
    } else {
      // Fallback: simple token refresh using JwtService
      log.warn("StatelessRefreshTokenService not available, using fallback refresh token logic");
      // Extract username from refresh token
      String username = jwtService.extractUsername(request.refreshToken());
      // Generate new tokens
      final var accessToken = jwtService.generateAccessToken(username);
      final var refreshToken = jwtService.generateRefreshToken(username);
      return AuthenticationResponseDto.builder()
          .accessToken(accessToken)
          .refreshToken(refreshToken)
          .tokenType("Bearer")
          .username(username)
          .build();
    }
  }

  @Transactional
  public void logout(final String refreshToken) {
    if (statelessRefreshTokenService != null) {
      statelessRefreshTokenService.revokeRefreshToken(refreshToken);
    } else {
      log.warn("StatelessRefreshTokenService not available, logout token revocation skipped");
    }
  }
}
