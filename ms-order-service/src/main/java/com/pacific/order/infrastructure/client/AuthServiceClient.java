package com.pacific.order.infrastructure.client;

import com.pacific.order.infrastructure.client.config.FeignClientConfig;
import com.pacific.order.infrastructure.client.dto.ValidateApiKeyRequest;
import com.pacific.order.infrastructure.client.dto.ValidateTokenRequest;
import com.pacific.order.infrastructure.client.dto.ValidateTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Feign client for Auth Service Used to validate JWT tokens */
@FeignClient(
    name = "auth-service",
    url = "${services.auth-service.url}",
    configuration = FeignClientConfig.class)
public interface AuthServiceClient {

  /**
   * Validate JWT token. Path must match auth-service's AuthenticationController
   * (@RequestMapping("/api/auth") + @PostMapping("/validate")) — the old /api/v1/auth/validate
   * 404'd on every call, so the fail-closed AuthValidationService returned "invalid" for every
   * token (visible as 401 "Invalid authentication token" on POST /orders).
   */
  @PostMapping("/api/auth/validate")
  ValidateTokenResponse validateToken(@RequestBody ValidateTokenRequest request);

  /**
   * Validate API key for service-to-service authentication. NOTE: auth-service has no
   * /api/auth/validate-api-key endpoint yet (no API-key store exists there) — this currently
   * 404s and fails closed (false → 401 INVALID_API_KEY). Kept in the contract so the X-API-Key
   * flow surfaces explicitly until auth implements the endpoint.
   */
  @PostMapping("/api/auth/validate-api-key")
  boolean validateApiKey(@RequestBody ValidateApiKeyRequest request);
}
