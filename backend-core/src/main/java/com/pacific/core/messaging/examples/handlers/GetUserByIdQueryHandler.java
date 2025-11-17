package com.pacific.core.messaging.examples.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.pacific.core.messaging.cqrs.query.QueryHandler;
import com.pacific.core.messaging.cqrs.query.QueryResult;
import com.pacific.core.messaging.examples.commands.UserDTO;
import com.pacific.core.messaging.examples.queries.GetUserByIdQuery;

/**
 * Example handler for GetUserByIdQuery. This demonstrates how to implement QueryHandler.
 *
 * <p>In a real application, this would: - Query the read model/database - Use caching for
 * performance - Return DTOs optimized for reading
 */
@Slf4j
@Component
public class GetUserByIdQueryHandler implements QueryHandler<GetUserByIdQuery, UserDTO> {

  @Override
  public QueryResult<UserDTO> handle(GetUserByIdQuery query) {
    try {
      log.debug("Handling GetUserByIdQuery for userId: {}", query.getUserId());

      // Simulate database query
      // In a real app, you would:
      // 1. Check cache first
      // 2. Query database if not in cache
      // 3. Transform entity to DTO
      // 4. Store in cache

      // Simulate finding user
      // User user = userRepository.findById(query.getUserId()).orElse(null);
      // if (user == null) {
      //     return QueryResult.empty();
      // }

      // Simulate user data
      UserDTO user =
          UserDTO.builder()
              .id(query.getUserId())
              .username("john.doe")
              .email("john.doe@example.com")
              .build();

      log.debug("User found: {}", user.getUsername());
      return QueryResult.of(user);

    } catch (Exception e) {
      log.error("Failed to get user: {}", e.getMessage(), e);
      return QueryResult.empty();
    }
  }
}
