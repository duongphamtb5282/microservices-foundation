package com.pacific.order.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for token validation
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidateTokenResponse {
    private boolean valid;
    private String userId;
    private String username;
    private String message;
}

