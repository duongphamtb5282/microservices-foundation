package com.pacific.core.messaging.examples.commands;

import lombok.Value;

import com.pacific.core.messaging.cqrs.command.Command;

/**
 * Example command for creating a user. This demonstrates how to implement the Command interface.
 */
@Value
public class CreateUserCommand implements Command<UserDTO> {

  String username;
  String email;
  String password;
  String initiator;
  String correlationId;

  @Override
  public String getCommandType() {
    return "CREATE_USER";
  }

  @Override
  public void validate() {
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("Username is required");
    }
    if (email == null || !email.contains("@")) {
      throw new IllegalArgumentException("Valid email is required");
    }
    if (password == null || password.length() < 8) {
      throw new IllegalArgumentException("Password must be at least 8 characters");
    }
  }
}
