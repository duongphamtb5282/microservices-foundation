package com.pacific.shared.events;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Foundation service events */
public class FoundationEvent {

  @Data
  @Builder
  @EqualsAndHashCode(callSuper = true)
  public static class CacheCleared extends BaseEvent {
    private String cacheName;
    private String reason;

    public CacheCleared() {
      super("CacheCleared", "foundation-service", "1.0");
    }

    public CacheCleared(String cacheName, String reason) {
      super("CacheCleared", "foundation-service", "1.0");
      this.cacheName = cacheName;
      this.reason = reason;
    }
  }

  @Data
  @Builder
  @EqualsAndHashCode(callSuper = true)
  public static class SystemHealthChanged extends BaseEvent {
    private String status;
    private String component;
    private String details;

    public SystemHealthChanged() {
      super("SystemHealthChanged", "foundation-service", "1.0");
    }

    public SystemHealthChanged(String status, String component, String details) {
      super("SystemHealthChanged", "foundation-service", "1.0");
      this.status = status;
      this.component = component;
      this.details = details;
    }
  }
}
