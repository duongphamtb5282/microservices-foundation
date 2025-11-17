package com.pacific.order.infrastructure.client;

import com.pacific.order.infrastructure.client.dto.ValidateApiKeyRequest;
import com.pacific.order.infrastructure.client.dto.ValidateTokenRequest;
import com.pacific.order.infrastructure.client.dto.ValidateTokenResponse;
import com.pacific.order.infrastructure.client.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for Auth Service
 * Used to validate JWT tokens
 */
@FeignClient(
    name = "auth-service",
    url = "${services.auth-service.url}",
    configuration = FeignClientConfig.class
)
public interface AuthServiceClient {

    /**
     * Validate JWT token
     */
    @PostMapping("/api/v1/auth/validate")
    ValidateTokenResponse validateToken(@RequestBody ValidateTokenRequest request);

    /**
     * Validate API key for service-to-service authentication
     */
    @PostMapping("/api/v1/auth/validate-api-key")
    boolean validateApiKey(@RequestBody ValidateApiKeyRequest request);
}

