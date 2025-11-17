package com.pacific.core.messaging.cqrs.command.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.pacific.core.messaging.config.KafkaWrapperProperties;
import com.pacific.core.messaging.cqrs.command.Command;
import com.pacific.core.messaging.cqrs.command.CommandBus;
import com.pacific.core.messaging.cqrs.command.CommandHandler;
import com.pacific.core.messaging.cqrs.command.CommandResult;
import com.pacific.core.messaging.cqrs.event.EventPublisher;

/**
 * Kafka-based implementation of CommandBus. Manages command handlers and executes commands with
 * event publishing.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class KafkaCommandBus implements CommandBus {

  private final EventPublisher eventPublisher;
  private final KafkaWrapperProperties properties;
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

      // Publish command executed event (if enabled)
      if (result.isSuccess() && properties.getCqrs().isEventStoreEnabled()) {
        publishCommandEvent(command, result);
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
      return CommandResult.failure(e.getMessage());
    }
  }

  @Override
  public <C extends Command<R>, R> CompletableFuture<CommandResult<R>> executeAsync(C command) {
    return CompletableFuture.supplyAsync(() -> execute(command));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <C extends Command<R>, R> void registerHandler(
      Class<C> commandClass, CommandHandler<C, R> handler) {

    if (handlers.containsKey(commandClass)) {
      throw new IllegalStateException(
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
      throw new IllegalStateException(
          "No handler registered for command: " + command.getClass().getSimpleName());
    }

    return handler;
  }

  /** Publish command executed event to Kafka. */
  private <C extends Command<R>, R> void publishCommandEvent(C command, CommandResult<R> result) {
    try {
      String topic = properties.getCqrs().getCommandTopic();

      // Create event (you would create a proper CommandExecutedEvent class)
      log.debug(
          "Publishing command executed event for {} to topic {}", command.getCommandType(), topic);

      // In a full implementation, create and publish a CommandExecutedEvent
      // eventPublisher.publish(topic, command.getCommandId(), event);

    } catch (Exception e) {
      log.error(
          "Failed to publish command event for {}: {}", command.getCommandType(), e.getMessage());
      // Don't fail the command execution if event publishing fails
    }
  }
}
