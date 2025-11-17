package com.pacific.core.messaging.cqrs.command;

/**
 * Handler for processing commands. Each command type should have exactly ONE handler (Single
 * Responsibility Principle).
 *
 * <p>Implementations should: - Execute business logic - Return success/failure result - Handle
 * exceptions gracefully - Be idempotent when possible
 *
 * @param <C> The command type this handler processes
 * @param <R> The result type returned after processing
 */
@FunctionalInterface
public interface CommandHandler<C extends Command<R>, R> {

  /**
   * Handle the command and return result.
   *
   * <p>This method should: 1. Execute business logic 2. Persist changes if needed 3. Return success
   * result with data, or failure result with error
   *
   * @param command The command to handle
   * @return CommandResult with success/failure status and optional data/error
   */
  CommandResult<R> handle(C command);
}
