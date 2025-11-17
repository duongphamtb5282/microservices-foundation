package com.pacific.core.cache.reloader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should trigger cache reloading when they expire. This can be used
 * with scheduled tasks or event listeners.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReloadCachesOnExpire {

  /**
   * The cache names to reload when this method is called. If empty, all caches will be reloaded.
   */
  String[] cacheNames() default {};

  /** Whether to reload all caches regardless of the cacheNames setting. */
  boolean reloadAll() default false;
}
