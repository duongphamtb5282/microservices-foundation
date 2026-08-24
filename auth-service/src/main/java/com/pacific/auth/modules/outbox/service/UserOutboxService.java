package com.pacific.auth.modules.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pacific.auth.common.exception.OutboxSerializationException;
import com.pacific.auth.modules.outbox.entity.UserOutboxEntity;
import com.pacific.auth.modules.outbox.repository.UserOutboxJpaRepository;
import com.pacific.shared.events.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Write side of the transactional outbox (ADR-0006). Called from within the registration
 * transaction so the outbox row commits atomically with the user.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserOutboxService {

  private final UserOutboxJpaRepository outboxRepository;
  private final ObjectMapper objectMapper;

  /** Serialize the event and persist a PENDING outbox row in the caller's transaction. */
  public void record(UserCreatedEvent event) {
    try {
      String payload = objectMapper.writeValueAsString(event);
      outboxRepository.save(UserOutboxEntity.from(event, payload));
      log.debug("Recorded outbox entry for event: {}", event.getUserId());
    } catch (JsonProcessingException e) {
      throw new OutboxSerializationException("Failed to serialize outbox event", e);
    }
  }
}
