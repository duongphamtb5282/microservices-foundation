package com.pacific.customer.application.command;

import com.pacific.customer.domain.model.CustomerPreferences;
import com.pacific.customer.domain.model.CustomerProfile;
import com.pacific.shared.messaging.cqrs.command.Command;
import java.util.Objects;

/** Command to create a new customer */
public record CreateCustomerCommand(
    String email, CustomerProfile profile, CustomerPreferences preferences, String initiator)
    implements Command {

  public CreateCustomerCommand {
    Objects.requireNonNull(email, "Email cannot be null");
    Objects.requireNonNull(profile, "Profile cannot be null");
    Objects.requireNonNull(preferences, "Preferences cannot be null");
    Objects.requireNonNull(initiator, "Initiator cannot be null");

    if (email.trim().isEmpty()) {
      throw new IllegalArgumentException("Email cannot be empty");
    }
    if (initiator.trim().isEmpty()) {
      throw new IllegalArgumentException("Initiator cannot be empty");
    }
    if (!email.contains("@")) {
      throw new IllegalArgumentException("Invalid email format");
    }
  }

  @Override
  public String getInitiator() {
    return initiator;
  }

  @Override
  public String getCommandId() {
    return "CREATE_CUSTOMER_" + java.util.UUID.randomUUID().toString();
  }

  @Override
  public String getCommandType() {
    return "CREATE_CUSTOMER";
  }
}
