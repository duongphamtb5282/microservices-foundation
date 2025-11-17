package com.pacific.core.messaging.cqrs.command;

import java.util.concurrent.CompletableFuture;

/**
 * Command Bus for dispatching commands to appropriate handlers.
 *
 * <p>Responsibilities: - Route commands to registered handlers - Support both synchronous and
 * asynchronous execution - Validate commands before execution - Handle cross-cutting concerns
 * (logging, metrics, tracing)
 */
public interface CommandBus {

  /**
   * Execute command synchronously. Blocks until command is processed and result is available.
   *
   * @param command The command to execute
   * @param <C> Command type
   * @param <R> Result type
   * @return CommandResult with success/failure status
   * @throws IllegalStateException if no handler is registered for the command
   */
  <C extends Command<R>, R> CommandResult<R> execute(C command);

  /**
   * Execute command asynchronously. Returns immediately with a CompletableFuture.
   *
   * @param command The command to execute
   * @param <C> Command type
   * @param <R> Result type
   * @return CompletableFuture that completes with CommandResult
   */
  <C extends Command<R>, R> CompletableFuture<CommandResult<R>> executeAsync(C command);

  /**
   * Register a command handler for a specific command type. Each command type should have exactly
   * one handler.
   *
   * @param commandClass The command class
   * @param handler The handler for this command type
   * @param <C> Command type
   * @param <R> Result type
   * @throws IllegalStateException if a handler is already registered
   */
  <C extends Command<R>, R> void registerHandler(
      Class<C> commandClass, CommandHandler<C, R> handler);
}
