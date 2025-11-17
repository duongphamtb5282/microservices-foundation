package com.pacific.customer.application.command;

import com.pacific.customer.domain.model.CustomerPreferences;
import com.pacific.customer.domain.model.CustomerProfile;
import com.pacific.shared.messaging.cqrs.command.Command;
import java.util.Objects;

/** Command to update an existing customer */
public record UpdateCustomerCommand(
    String customerId, CustomerProfile profile, CustomerPreferences preferences, String initiator)
    implements Command {

  public UpdateCustomerCommand {
    Objects.requireNonNull(customerId, "Customer ID cannot be null");
    Objects.requireNonNull(initiator, "Initiator cannot be null");

    if (customerId.trim().isEmpty()) {
      throw new IllegalArgumentException("Customer ID cannot be empty");
    }
    if (initiator.trim().isEmpty()) {
      throw new IllegalArgumentException("Initiator cannot be empty");
    }
    if (profile == null && preferences == null) {
      throw new IllegalArgumentException("At least profile or preferences must be provided");
    }
  }

  @Override
  public String getInitiator() {
    return initiator;
  }

  @Override
  public String getCommandId() {
    return "UPDATE_CUSTOMER_" + customerId + "_" + java.util.UUID.randomUUID().toString();
  }

  @Override
  public String getCommandType() {
    return "UPDATE_CUSTOMER";
  }

  /** Checks if profile update is requested */
  public boolean hasProfileUpdate() {
    return profile != null;
  }

  /** Checks if preferences update is requested */
  public boolean hasPreferencesUpdate() {
    return preferences != null;
  }
}
