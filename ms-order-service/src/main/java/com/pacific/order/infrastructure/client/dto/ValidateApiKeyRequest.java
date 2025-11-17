package com.pacific.order.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for API key validation
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidateApiKeyRequest {
    private String apiKey;
}
