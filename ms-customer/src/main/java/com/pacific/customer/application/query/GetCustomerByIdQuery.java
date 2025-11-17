package com.pacific.customer.application.query;

import com.pacific.shared.messaging.cqrs.query.Query;
import java.util.Objects;

/** Query to get customer by ID */
public record GetCustomerByIdQuery(String customerId) implements Query {

  public GetCustomerByIdQuery {
    Objects.requireNonNull(customerId, "Customer ID cannot be null");
    if (customerId.trim().isEmpty()) {
      throw new IllegalArgumentException("Customer ID cannot be empty");
    }
  }

  @Override
  public String getCorrelationId() {
    return "GET_CUSTOMER_" + customerId;
  }

  @Override
  public String getQueryType() {
    return "GET_CUSTOMER_BY_ID";
  }

  @Override
  public void validate() {
    // Additional validation can be added here
    if (customerId.length() > 50) {
      throw new IllegalArgumentException("Customer ID is too long");
    }
  }
}
