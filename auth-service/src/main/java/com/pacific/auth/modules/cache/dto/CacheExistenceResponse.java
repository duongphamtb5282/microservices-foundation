package com.pacific.auth.modules.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for cache existence check. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheExistenceResponse {
  private boolean exists;
  private String cacheName;
}
