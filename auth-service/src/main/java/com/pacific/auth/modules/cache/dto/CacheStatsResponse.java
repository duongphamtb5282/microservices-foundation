package com.pacific.auth.modules.cache.dto;

import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for cache statistics. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStatsResponse {
  private int totalCaches;
  private Collection<String> cacheNames;
  private int redisKeyCount;
}
