package com.pacific.core.messaging.cqrs.command.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.pacific.core.messaging.config.KafkaWrapperProperties;
import com.pacific.core.messaging.cqrs.command.Command;
import com.pacific.core.messaging.cqrs.command.CommandBus;
import com.pacific.core.messaging.cqrs.command.CommandBusException;
import com.pacific.core.messaging.cqrs.command.CommandHandler;
import com.pacific.core.messaging.cqrs.command.CommandResult;
import com.pacific.core.messaging.cqrs.event.EventPublisher;

/**
 * Kafka-based implementation of CommandBus. Manages command handlers and executes commands with
 * event publishing.
 */
@Slf4j
@Component
// Kafka-backed CQRS: register only where spring-kafka is on the classpath (no kafka.enabled
// property gate — the classpath is the gate). Same sentinel as KafkaEventPublisher.
@ConditionalOnClass(KafkaTemplate.class)
@RequiredArgsConstructor
public class KafkaCommandBus implements CommandBus {

  private final EventPublisher eventPublisher;
  private final KafkaWrapperProperties properties;

  @Qualifier("coreAsyncExecutor")
  private final Executor coreAsyncExecutor;

  private final Map<Class<?>, CommandHandler<?, ?>> handlers = new ConcurrentHashMap<>();

  @Override
  public <C extends Command<R>, R> CommandResult<R> execute(C command) {
    log.info("Executing command: {} (id: {})", command.getCommandType(), command.getCommandId());

    long startTime = System.currentTimeMillis();

    try {
      // Validate command
      command.validate();

      // Get handler
      CommandHandler<C, R> handler = getHandler(command);

      // Execute command
      CommandResult<R> result = handler.handle(command);

      long duration = System.currentTimeMillis() - startTime;
      log.info(
          "Command {} executed in {}ms (success: {})",
          command.getCommandType(),
          duration,
          result.isSuccess());

      // Command events are not published (F-01 - topic unused; CQRS is read-path only)
      if (result.isSuccess() && properties.getCqrs().isEventStoreEnabled()) {
        log.debug("Event store enabled but command event publishing is disabled (F-01)");
      }

      return result;

    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error(
          "Failed to execute command {} after {}ms: {}",
          command.getCommandType(),
          duration,
          e.getMessage(),
          e);
      // Sanitize: do not leak internal exception messages to API consumers (6a)
      return CommandResult.failure("Unexpected error processing command", "INTERNAL_ERROR");
    }
  }

  @Override
  public <C extends Command<R>, R> CompletableFuture<CommandResult<R>> executeAsync(C command) {
    // ADR-0011: run on the shared core-async executor (virtual threads) — the default
    // supplyAsync() would land on ForkJoinPool.commonPool(), shared and starvation-prone.
    return CompletableFuture.supplyAsync(() -> execute(command), coreAsyncExecutor);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <C extends Command<R>, R> void registerHandler(
      Class<C> commandClass, CommandHandler<C, R> handler) {

    if (handlers.containsKey(commandClass)) {
      throw new CommandBusException(
          "Handler already registered for command: " + commandClass.getSimpleName());
    }

    handlers.put(commandClass, handler);
    log.info("Registered command handler for: {}", commandClass.getSimpleName());
  }

  /** Get handler for command. */
  @SuppressWarnings("unchecked")
  private <C extends Command<R>, R> CommandHandler<C, R> getHandler(C command) {
    CommandHandler<C, R> handler = (CommandHandler<C, R>) handlers.get(command.getClass());

    if (handler == null) {
      throw new CommandBusException(
          "No handler registered for command: " + command.getClass().getSimpleName());
    }

    return handler;
  }
}
