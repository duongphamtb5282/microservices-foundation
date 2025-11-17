package com.pacific.payment.config;

import com.pacific.core.messaging.cqrs.command.CommandBus;
import com.pacific.payment.modules.payment.command.CreatePaymentCommand;
import com.pacific.payment.modules.payment.handler.CreatePaymentCommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to register CQRS handlers
 * Runs on application startup
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class HandlerRegistrationConfig implements CommandLineRunner {

    private final CommandBus commandBus;

    // Command Handlers
    private final CreatePaymentCommandHandler createPaymentHandler;

    @Override
    public void run(String... args) {
        log.info("Registering CQRS handlers for Payment Service");

        // Register Command Handlers
        commandBus.registerHandler(CreatePaymentCommand.class, createPaymentHandler);

        log.info("Successfully registered {} command handlers", 1);
    }
}

