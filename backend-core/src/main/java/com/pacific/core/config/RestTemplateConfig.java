package com.pacific.core.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.pacific.core.client.RestTemplateInterceptor;

@Configuration
// Blocking RestTemplate only makes sense in servlet (blocking) apps. Boot's own
// RestTemplateAutoConfiguration carries @Conditional(NotReactiveWebApplicationCondition) and is
// skipped in reactive apps like the gateway, so RestTemplateBuilder never exists there — this
// bean would fail with "no bean of type RestTemplateBuilder". Mirror Boot's polarity with the
// SERVLET gate.
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
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
