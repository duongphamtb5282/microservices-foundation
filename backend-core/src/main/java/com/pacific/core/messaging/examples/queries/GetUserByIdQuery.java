package com.pacific.core.messaging.examples.queries;

import lombok.Value;

import com.pacific.core.messaging.cqrs.query.Query;
import com.pacific.core.messaging.examples.commands.UserDTO;

/**
 * Example query for retrieving a user by ID. This demonstrates how to implement the Query
 * interface.
 */
@Value
public class GetUserByIdQuery implements Query<UserDTO> {

  String userId;
  String correlationId;

  @Override
  public String getQueryType() {
    return "GET_USER_BY_ID";
  }

  @Override
  public String getCacheKey() {
    return "user:" + userId;
  }
}
