package com.pacific.order.config;

import com.pacific.core.messaging.cqrs.command.CommandBus;
import com.pacific.core.messaging.cqrs.query.QueryBus;
import lombok.extern.slf4j.Slf4j;
import com.pacific.order.application.command.CancelOrderCommand;
import com.pacific.order.application.command.CreateOrderCommand;
import com.pacific.order.application.handler.CancelOrderCommandHandler;
import com.pacific.order.application.handler.CreateOrderCommandHandler;
import com.pacific.order.application.handler.GetOrderByIdQueryHandler;
import com.pacific.order.application.handler.GetOrdersByUserQueryHandler;
import com.pacific.order.application.query.GetOrderByIdQuery;
import com.pacific.order.application.query.GetOrdersByUserQuery;
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
    private final QueryBus queryBus;

    // Command Handlers
    private final CreateOrderCommandHandler createOrderHandler;
    private final CancelOrderCommandHandler cancelOrderHandler;

    // Query Handlers
    private final GetOrderByIdQueryHandler getOrderByIdHandler;
    private final GetOrdersByUserQueryHandler getOrdersByUserHandler;

    @Override
    public void run(String... args) {
        log.info("Registering CQRS handlers for Order Service");

        // Register Command Handlers
        commandBus.registerHandler(CreateOrderCommand.class, createOrderHandler);
        commandBus.registerHandler(CancelOrderCommand.class, cancelOrderHandler);

        // Register Query Handlers
        queryBus.registerHandler(GetOrderByIdQuery.class, getOrderByIdHandler);
        queryBus.registerHandler(GetOrdersByUserQuery.class, getOrdersByUserHandler);

        log.info("Successfully registered {} command handlers and {} query handlers",
                2, 2);
    }
}

