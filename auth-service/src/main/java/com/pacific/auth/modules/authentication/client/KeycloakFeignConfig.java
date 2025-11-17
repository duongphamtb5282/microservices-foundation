package com.pacific.auth.modules.authentication.client;

import feign.Logger;
import feign.codec.ErrorDecoder;
import feign.form.FormEncoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Feign configuration for Keycloak clients Handles encoding, error handling, and logging */
@Configuration
public class KeycloakFeignConfig {

  /** Set Feign logging level */
  @Bean
  public Logger.Level feignLoggerLevel() {
    return Logger.Level.FULL; // Log all requests/responses for debugging
  }

  /** Custom error decoder for Keycloak errors */
  @Bean
  public ErrorDecoder errorDecoder() {
    return new KeycloakErrorDecoder();
  }

  /** Form encoder for URL-encoded requests Required for Keycloak token endpoint */
  @Bean
  public FormEncoder formEncoder(ObjectFactory<HttpMessageConverters> converters) {
    return new FormEncoder(new SpringEncoder(converters));
  }
}
