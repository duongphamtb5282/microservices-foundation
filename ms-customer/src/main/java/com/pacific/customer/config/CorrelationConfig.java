package com.pacific.customer.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration for correlation ID in WebFlux application.
 * Correlation ID handling is provided by the CorrelationIdFilter.
 */
@Configuration
public class CorrelationConfig {
    // Correlation ID handling is done via WebFilter
    // Metrics can be added later if needed with proper AspectJ setup
}
