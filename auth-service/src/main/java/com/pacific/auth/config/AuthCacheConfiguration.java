package com.pacific.auth.config;

import com.pacific.core.cache.reloader.CacheReloader;
import com.pacific.core.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AuthCacheConfiguration {

  private final CacheService cacheService;
  private final CacheReloader cacheReloader;

  @Value("${auth.cache.reload-on-startup:false}")
  private boolean reloadOnStartup;

  @Value("${auth.cache.scheduled-reload-enabled:true}")
  private boolean scheduledReloadEnabled;

  @Value("${auth.cache.scheduled-reload-interval-ms:300000}")
  private long scheduledReloadIntervalMs;

  public static final String USER_CACHE = "users";
  public static final String ROLE_CACHE = "roles";
  public static final String TOKEN_CACHE = "tokens";
  public static final String PERMISSION_CACHE = "permissions";

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    if (reloadOnStartup) {
      log.info("🔄 Application ready - performing initial cache reload");
      try {
        manualCacheReload();
      } catch (Exception e) {
        log.error("❌ Error during startup cache reload: {}", e.getMessage());
      }
    } else {
      log.info("⏭️ Skipping startup cache reload (auth.cache.reload-on-startup=false)");
    }
  }

  /**
   * Reload all auth caches. This is the single entry point used by the manual reload endpoint
   * ({@code POST /api/cache/reload/auth/all}) and the scheduled reload task. A manual reload is an
   * explicit admin action and is never gated by {@code scheduledReloadEnabled} — that flag only
   * controls the periodic task.
   */
  public void manualCacheReload() {
    log.info("🔄 Manual cache reload for auth-service");
    long startTime = System.currentTimeMillis();

    cacheReloader.reloadCache(USER_CACHE);
    cacheReloader.reloadCache(ROLE_CACHE);
    cacheReloader.reloadCache(TOKEN_CACHE);
    cacheReloader.reloadCache(PERMISSION_CACHE);

    long duration = System.currentTimeMillis() - startTime;
    log.info("✅ Auth-service caches reloaded in {}ms", duration);
  }

  /** Scheduled reload task — the only consumer of {@code scheduledReloadEnabled}. */
  @Scheduled(fixedDelayString = "${auth.cache.scheduled-reload-interval-ms:300000}")
  public void scheduledCacheReload() {
    if (!scheduledReloadEnabled) {
      log.debug("⏭️ Skipping scheduled cache reload (auth.cache.scheduled-reload-enabled=false)");
      return;
    }
    log.info("🔄 Scheduled cache reload (interval: {}ms)", scheduledReloadIntervalMs);
    manualCacheReload();
  }

  public void clearAuthCaches() {
    log.info("🗑️ Clearing all auth-service caches");
    cacheService.clear(USER_CACHE);
    cacheService.clear(ROLE_CACHE);
    cacheService.clear(TOKEN_CACHE);
    cacheService.clear(PERMISSION_CACHE);
    log.info("✅ All auth-service caches cleared");
  }
}
