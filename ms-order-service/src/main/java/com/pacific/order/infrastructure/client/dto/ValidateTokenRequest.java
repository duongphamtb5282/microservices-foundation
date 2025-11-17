package com.pacific.order.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for token validation
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidateTokenRequest {
    private String token;
}

