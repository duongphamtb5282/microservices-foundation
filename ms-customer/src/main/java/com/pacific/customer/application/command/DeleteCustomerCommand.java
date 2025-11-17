package com.pacific.customer.application.command;

import com.pacific.shared.messaging.cqrs.command.Command;
import java.util.Objects;

/** Command to delete a customer (soft delete by changing status to CLOSED) */
public record DeleteCustomerCommand(String customerId, String reason, String initiator)
    implements Command {

  public DeleteCustomerCommand {
    Objects.requireNonNull(customerId, "Customer ID cannot be null");
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
    return "DELETE_CUSTOMER_" + customerId + "_" + java.util.UUID.randomUUID().toString();
  }

  @Override
  public String getCommandType() {
    return "DELETE_CUSTOMER";
  }
}
