package com.pacific.core.config;

import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.pacific.core.client.RestTemplateInterceptor;

@Configuration
public class RestTemplateConfig {

  private final RestTemplateInterceptor restTemplateInterceptor;

  public RestTemplateConfig(RestTemplateInterceptor restTemplateInterceptor) {
    this.restTemplateInterceptor = restTemplateInterceptor;
  }

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .setConnectTimeout(Duration.ofSeconds(5))
        .setReadTimeout(Duration.ofSeconds(10))
        .additionalInterceptors(restTemplateInterceptor)
        .build();
  }
}
