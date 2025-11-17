package com.pacific.auth.modules.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for cache health check. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheHealthResponse {
  private String status;
  private boolean userCacheExists;
  private boolean roleCacheExists;
  private boolean tokenCacheExists;
  private boolean permissionCacheExists;
}
