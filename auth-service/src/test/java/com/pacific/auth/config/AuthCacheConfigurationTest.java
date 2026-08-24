package com.pacific.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.pacific.core.cache.reloader.CacheReloader;
import com.pacific.core.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Regression test: manualCacheReload is the single canonical reload entry point (used by the
 * manual reload endpoint and the scheduled task), and the scheduledReloadEnabled flag only gates
 * the scheduled task — never a manual reload.
 */
class AuthCacheConfigurationTest {

  private CacheService cacheService;
  private CacheReloader cacheReloader;
  private AuthCacheConfiguration config;

  @BeforeEach
  void setUp() {
    cacheService = mock(CacheService.class);
    cacheReloader = mock(CacheReloader.class);
    config = new AuthCacheConfiguration(cacheService, cacheReloader);
    ReflectionTestUtils.setField(config, "scheduledReloadEnabled", true);
    ReflectionTestUtils.setField(config, "reloadOnStartup", false);
  }

  @Test
  void manualCacheReload_reloadsAllFourAuthCaches() {
    config.manualCacheReload();

    verify(cacheReloader).reloadCache(AuthCacheConfiguration.USER_CACHE);
    verify(cacheReloader).reloadCache(AuthCacheConfiguration.ROLE_CACHE);
    verify(cacheReloader).reloadCache(AuthCacheConfiguration.TOKEN_CACHE);
    verify(cacheReloader).reloadCache(AuthCacheConfiguration.PERMISSION_CACHE);
    verify(cacheService, never()).clear(anyString());
  }

  @Test
  void manualCacheReload_neverGatedByScheduledFlag() {
    ReflectionTestUtils.setField(config, "scheduledReloadEnabled", false);

    config.manualCacheReload();

    verify(cacheReloader, times(4)).reloadCache(anyString());
  }

  @Test
  void scheduledCacheReload_disabled_skipsReload() {
    ReflectionTestUtils.setField(config, "scheduledReloadEnabled", false);

    config.scheduledCacheReload();

    verify(cacheReloader, never()).reloadCache(anyString());
  }

  @Test
  void scheduledCacheReload_enabled_reloadsAllCaches() {
    config.scheduledCacheReload();

    verify(cacheReloader, times(4)).reloadCache(anyString());
  }

  @Test
  void cacheNames_areStable() {
    assertThat(AuthCacheConfiguration.USER_CACHE).isEqualTo("users");
    assertThat(AuthCacheConfiguration.ROLE_CACHE).isEqualTo("roles");
    assertThat(AuthCacheConfiguration.TOKEN_CACHE).isEqualTo("tokens");
    assertThat(AuthCacheConfiguration.PERMISSION_CACHE).isEqualTo("permissions");
  }
}
