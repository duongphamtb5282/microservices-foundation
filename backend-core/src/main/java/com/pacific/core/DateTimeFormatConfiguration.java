package com.pacific.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;

/** Configure the converters to use the ISO format for dates by default. */
@Configuration
public class DateTimeFormatConfiguration {

  @Bean
  public FormattingConversionService formattingConversionService() {
    DefaultFormattingConversionService service = new DefaultFormattingConversionService();
    DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
    registrar.setUseIsoFormat(true);
    registrar.registerFormatters(service);
    return service;
  }
}
