package com.pacific.core.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CorrelationConfig {

  /** Enable @Timed annotations for metrics */
  @Bean
  // TimedAspect implements org.aspectj.lang.annotation.Aspect (aspectjrt); the gateway has no
  // aspectj on its classpath (NCDFE NoAspectBoundException at startup), so only register the
  // aspect where AOP is available. The correlationMetricsConfigurer below stays ungated — it is
  // already reflection-guarded against missing servlet MVC types.
  @ConditionalOnClass(Aspect.class)
  public TimedAspect timedAspect(MeterRegistry registry) {
    return new TimedAspect(registry);
  }

  /**
   * If Spring MVC is present, register a WebMvcConfigurer that adds the
   * CorrelationMetricsInterceptor. We avoid compile-time references to servlet MVC types so this
   * class can be loaded in reactive apps.
   */
  @Bean
  public Object correlationMetricsConfigurer() {
    try {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      Class<?> webMvcConfigurerClass =
          Class.forName(
              "org.springframework.web.servlet.config.annotation.WebMvcConfigurer", false, cl);
      Class<?> interceptorRegistryClass =
          Class.forName(
              "org.springframework.web.servlet.config.annotation.InterceptorRegistry", false, cl);
      Class<?> handlerInterceptorClass =
          Class.forName("org.springframework.web.servlet.HandlerInterceptor", false, cl);

      final Object interceptorInstance =
          new com.pacific.core.interceptor.CorrelationMetricsInterceptor();

      Object proxy =
          java.lang.reflect.Proxy.newProxyInstance(
              webMvcConfigurerClass.getClassLoader(),
              new Class<?>[] {webMvcConfigurerClass},
              (proxyObj, method, args) -> {
                if ("addInterceptors".equals(method.getName())
                    && args != null
                    && args.length == 1) {
                  Object registry = args[0];
                  java.lang.reflect.Method addInterceptor =
                      registry.getClass().getMethod("addInterceptor", handlerInterceptorClass);
                  addInterceptor.invoke(registry, interceptorInstance);
                  return null;
                }
                // InvocationHandler contract: every method must return a value of its return
                // type, including the Object methods. Spring calls hashCode()/equals() on every
                // singleton bean during creation (PersistenceAnnotationBeanPostProcessor.
                // requiresDestruction caches beans in a ConcurrentHashMap) and NPEs when the
                // handler returns null — the startup failure this fixes.
                if ("hashCode".equals(method.getName())) {
                  return System.identityHashCode(proxyObj);
                }
                if ("equals".equals(method.getName())) {
                  return proxyObj == args[0];
                }
                if ("toString".equals(method.getName())) {
                  return "correlationMetricsConfigurer@" + System.identityHashCode(proxyObj);
                }
                return null;
              });

      return proxy;
    } catch (ClassNotFoundException e) {
      // Spring MVC not present — nothing to do
      return null;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to register correlation metrics configurer", e);
    }
  }
}
