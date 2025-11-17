package com.pacific.core.messaging.examples.handlers;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.pacific.core.messaging.cqrs.command.CommandHandler;
import com.pacific.core.messaging.cqrs.command.CommandResult;
import com.pacific.core.messaging.examples.commands.CreateUserCommand;
import com.pacific.core.messaging.examples.commands.UserDTO;

/**
 * Example handler for CreateUserCommand. This demonstrates how to implement CommandHandler.
 *
 * <p>In a real application, this would: - Interact with repositories - Call business logic services
 * - Publish domain events
 */
@Slf4j
@Component
public class CreateUserCommandHandler implements CommandHandler<CreateUserCommand, UserDTO> {

  @Override
  public CommandResult<UserDTO> handle(CreateUserCommand command) {
    try {
      log.info("Handling CreateUserCommand for username: {}", command.getUsername());

      // Simulate business logic
      // In a real app, you would:
      // 1. Check if user already exists
      // 2. Hash the password
      // 3. Save to database
      // 4. Publish UserCreatedEvent

      // Simulate checking if user exists
      // if (userRepository.existsByUsername(command.getUsername())) {
      //     return CommandResult.failure("User already exists", "USER_ALREADY_EXISTS");
      // }

      // Create user DTO (simulating saved entity)
      UserDTO user =
          UserDTO.builder()
              .id(UUID.randomUUID().toString())
              .username(command.getUsername())
              .email(command.getEmail())
              .build();

      log.info("User created successfully: {}", user.getId());
      return CommandResult.success(user);

    } catch (Exception e) {
      log.error("Failed to create user: {}", e.getMessage(), e);
      return CommandResult.failure(
          "Failed to create user: " + e.getMessage(), "USER_CREATION_FAILED");
    }
  }
}
