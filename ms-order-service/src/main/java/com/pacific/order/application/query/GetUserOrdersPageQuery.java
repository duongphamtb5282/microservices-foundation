package com.pacific.order.application.query;

import com.pacific.core.messaging.cqrs.query.Query;
import com.pacific.order.application.dto.OrderResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

/**
 * DB-side paginated query for a user's orders (F-19). Replaces the V2 controller's in-memory {@code
 * PageImpl} slicing of an unbounded list. Page/size are clamped in {@link #validate()}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetUserOrdersPageQuery implements Query<Page<OrderResponse>> {

  /** Hard cap on page size so one request cannot pull the whole table (F-19/F-20). */
  public static final int MAX_PAGE_SIZE = 100;

  private String userId;
  private int page;
  private int size;
  private String correlationId;

  @Override
  public String getQueryType() {
    return "GET_USER_ORDERS_PAGE";
  }

  @Override
  public String getCorrelationId() {
    return correlationId;
  }

  public void validate() {
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("User ID is required");
    }
    if (page < 0) {
      throw new IllegalArgumentException("Page must be >= 0");
    }
    if (size < 1 || size > MAX_PAGE_SIZE) {
      throw new IllegalArgumentException("Size must be between 1 and " + MAX_PAGE_SIZE);
    }
  }
}
