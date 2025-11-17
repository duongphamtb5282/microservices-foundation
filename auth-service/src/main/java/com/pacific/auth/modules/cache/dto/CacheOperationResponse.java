package com.pacific.auth.modules.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for cache operations (reload, clear, etc). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheOperationResponse {
  private String status;
  private String message;
  private String cacheName;
}
