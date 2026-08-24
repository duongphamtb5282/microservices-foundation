package com.pacific.order.infrastructure.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pacific.order.domain.event.OrderCancelledEvent;
import com.pacific.order.domain.event.OrderCreatedEvent;
import com.pacific.order.infrastructure.exception.OutboxSerializationException;
import com.pacific.order.infrastructure.outbox.entity.OrderOutboxEntity;
import com.pacific.order.infrastructure.outbox.repository.OrderOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Write side of the transactional outbox (ADR-0001). Called from within the order transaction so
 * the outbox row commits atomically with the order.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderOutboxService {

  private final OrderOutboxJpaRepository outboxRepository;
  private final ObjectMapper objectMapper;

  /** Serialize the event and persist a PENDING outbox row in the caller's transaction. */
  public void record(OrderCreatedEvent event) {
    try {
      String payload = objectMapper.writeValueAsString(event);
      outboxRepository.save(OrderOutboxEntity.from(event, payload));
      log.debug("Recorded outbox entry for event: {}", event.getOrderId());
    } catch (JsonProcessingException e) {
      throw new OutboxSerializationException("Failed to serialize outbox event", e);
    }
  }

  /**
   * Serialize a cancellation event and persist a PENDING outbox row in the caller's transaction
   * (ADR-0007 saga compensation — the cancellation commits atomically with the order status).
   */
  public void record(OrderCancelledEvent event) {
    try {
      String payload = objectMapper.writeValueAsString(event);
      outboxRepository.save(OrderOutboxEntity.from(event, payload));
      log.debug("Recorded outbox entry for cancellation: {}", event.getOrderId());
    } catch (JsonProcessingException e) {
      throw new OutboxSerializationException("Failed to serialize outbox event", e);
    }
  }
}
