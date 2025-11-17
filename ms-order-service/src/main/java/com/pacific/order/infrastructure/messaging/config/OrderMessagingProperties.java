package com.pacific.order.infrastructure.messaging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for order messaging
 */
@Configuration
@ConfigurationProperties(prefix = "order.messaging")
@Data
public class OrderMessagingProperties {
    
    private String orderEventsTopic = "order.events";
    private String orderCommandsTopic = "order.commands";
}

