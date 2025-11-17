package com.pacific.customer.application.command;

import com.pacific.customer.domain.model.CustomerStatus;
import com.pacific.shared.messaging.cqrs.command.Command;
import java.util.Objects;

/** Command to update customer status */
public record UpdateCustomerStatusCommand(
    String customerId, CustomerStatus newStatus, String reason, String initiator)
    implements Command {

  public UpdateCustomerStatusCommand {
    Objects.requireNonNull(customerId, "Customer ID cannot be null");
    Objects.requireNonNull(newStatus, "New status cannot be null");
    Objects.requireNonNull(initiator, "Initiator cannot be null");

    if (customerId.trim().isEmpty()) {
      throw new IllegalArgumentException("Customer ID cannot be empty");
    }
    if (initiator.trim().isEmpty()) {
      throw new IllegalArgumentException("Initiator cannot be empty");
    }
  }

  @Override
  public String getInitiator() {
    return initiator;
  }

  @Override
  public String getCommandId() {
    return "UPDATE_STATUS_" + customerId + "_" + java.util.UUID.randomUUID().toString();
  }

  @Override
  public String getCommandType() {
    return "UPDATE_CUSTOMER_STATUS";
  }

  /** Checks if this is an activation command */
  public boolean isActivation() {
    return newStatus == CustomerStatus.ACTIVE;
  }

  /** Checks if this is a deactivation command */
  public boolean isDeactivation() {
    return newStatus != CustomerStatus.ACTIVE;
  }

  /** Checks if this is a suspension */
  public boolean isSuspension() {
    return newStatus == CustomerStatus.SUSPENDED;
  }
}
