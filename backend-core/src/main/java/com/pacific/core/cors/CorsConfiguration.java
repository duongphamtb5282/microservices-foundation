package com.pacific.core.cors;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Register a servlet `Filter` for CORS only when the Servlet API is present. This avoids
 * compile-time references to `jakarta.servlet` so the configuration class can be loaded in reactive
 * (WebFlux) applications that don't have the servlet API on the classpath.
 */
@Configuration
@ConditionalOnClass(name = "jakarta.servlet.Filter")
@Slf4j
@RequiredArgsConstructor
public class CorsConfiguration {

  private final CorsConfigurationHelper corsConfigurationHelper;

  @Bean
  public Object corsFilterRegistration() {
    try {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();

      Class<?> filterClass = Class.forName("jakarta.servlet.Filter", false, cl);
      Class<?> servletRequestClass = Class.forName("jakarta.servlet.ServletRequest", false, cl);
      Class<?> servletResponseClass = Class.forName("jakarta.servlet.ServletResponse", false, cl);
      Class<?> filterChainClass = Class.forName("jakarta.servlet.FilterChain", false, cl);
      Class<?> httpServletRequestClass =
          Class.forName("jakarta.servlet.http.HttpServletRequest", false, cl);
      Class<?> httpServletResponseClass =
          Class.forName("jakarta.servlet.http.HttpServletResponse", false, cl);

      // Create a dynamic Filter implementation that delegates to CorsConfigurationHelper
      Object filterProxy =
          Proxy.newProxyInstance(
              filterClass.getClassLoader(),
              new Class<?>[] {filterClass},
              (proxy, method, args) -> {
                String name = method.getName();
                if ("init".equals(name)) {
                  log.info("🔧 Initializing CORS filter with shared configuration");
                  return null;
                }
                if ("destroy".equals(name)) {
                  log.info("🔧 CORS filter destroyed");
                  return null;
                }
                if ("doFilter".equals(name) && args != null && args.length == 3) {
                  Object req = args[0];
                  Object res = args[1];
                  Object chain = args[2];

                  // applyCorsHeaders(HttpServletRequest, HttpServletResponse)
                  Method apply =
                      corsConfigurationHelper
                          .getClass()
                          .getMethod(
                              "applyCorsHeaders",
                              httpServletRequestClass,
                              httpServletResponseClass);
                  apply.invoke(corsConfigurationHelper, req, res);

                  // if preflight: handlePreflightRequest(HttpServletResponse) and return
                  Method isPre =
                      corsConfigurationHelper
                          .getClass()
                          .getMethod("isPreflightRequest", httpServletRequestClass);
                  Boolean pre = (Boolean) isPre.invoke(corsConfigurationHelper, req);
                  if (pre != null && pre) {
                    Method handle =
                        corsConfigurationHelper
                            .getClass()
                            .getMethod("handlePreflightRequest", httpServletResponseClass);
                    handle.invoke(corsConfigurationHelper, res);
                    return null;
                  }

                  // otherwise continue filter chain: filterChain.doFilter(req, res)
                  Method doFilter =
                      chain
                          .getClass()
                          .getMethod("doFilter", servletRequestClass, servletResponseClass);
                  doFilter.invoke(chain, req, res);
                  return null;
                }
                // InvocationHandler contract: hashCode/equals/toString must return typed values,
                // not null — containers and Spring call them on arbitrary beans (same fix as
                // CorrelationConfig.correlationMetricsConfigurer).
                if ("hashCode".equals(name)) {
                  return System.identityHashCode(proxy);
                }
                if ("equals".equals(name)) {
                  return proxy == args[0];
                }
                if ("toString".equals(name)) {
                  return "corsFilterProxy@" + System.identityHashCode(proxy);
                }
                return null;
              });

      // Try to register via FilterRegistrationBean if available so we can set order
      try {
        Class<?> frbClass =
            Class.forName("org.springframework.boot.web.servlet.FilterRegistrationBean", false, cl);
        Object frb = frbClass.getConstructor().newInstance();
        Method setFilter = frbClass.getMethod("setFilter", filterClass);
        setFilter.invoke(frb, filterProxy);
        Method setOrder = frbClass.getMethod("setOrder", int.class);
        setOrder.invoke(frb, Ordered.HIGHEST_PRECEDENCE);
        return frb;
      } catch (ClassNotFoundException cnfe) {
        // FilterRegistrationBean not available; return raw filter proxy instead
        return filterProxy;
      }

    } catch (Exception e) {
      throw new IllegalStateException("Failed to register servlet CORS filter", e);
    }
  }
}
