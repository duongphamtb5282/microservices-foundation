package com.pacific.order.application.handler;

import com.pacific.core.messaging.cqrs.query.QueryHandler;
import com.pacific.core.messaging.cqrs.query.QueryResult;
import com.pacific.order.application.dto.OrderResponse;
import com.pacific.order.application.mapper.OrderMapper;
import com.pacific.order.application.query.GetUserOrdersPageQuery;
import com.pacific.order.domain.model.Order;
import com.pacific.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Handler for {@link GetUserOrdersPageQuery} — real DB-side pagination (F-19). Items are
 * eager-fetched by the repository (F-18), so mapping is safe outside a transaction.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetUserOrdersPageQueryHandler
    implements QueryHandler<GetUserOrdersPageQuery, Page<OrderResponse>> {

  private final OrderRepository orderRepository;

  @Override
  @Cacheable(value = "user-orders", key = "#query.userId + ':' + #query.page + ':' + #query.size")
  public QueryResult<Page<OrderResponse>> handle(GetUserOrdersPageQuery query) {
    try {
      query.validate();
      log.debug(
          "Handling GetUserOrdersPageQuery for user: {} (page: {}, size: {})",
          query.getUserId(),
          query.getPage(),
          query.getSize());

      Page<Order> orders =
          orderRepository.findByUserId(
              query.getUserId(), PageRequest.of(query.getPage(), query.getSize()));

      Page<OrderResponse> responses = orders.map(OrderMapper::toResponse);

      log.debug(
          "Found {} orders for user: {} (page {}/{}, cached)",
          responses.getNumberOfElements(),
          query.getUserId(),
          query.getPage(),
          responses.getTotalPages());
      return QueryResult.of(responses);

    } catch (IllegalArgumentException e) {
      // Invalid paging parameters are a client error, not an infrastructure failure
      throw e;
    } catch (Exception e) {
      // Do not mask a DB failure as an empty page — rethrow; the query bus surfaces it as a 500
      log.error("Failed to get orders page for user: {}", query.getUserId(), e);
      throw e;
    }
  }
}
