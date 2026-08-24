package com.pacific.payment.modules.payment.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pacific.payment.modules.payment.event.PaymentResultEvent;
import com.pacific.payment.modules.payment.outbox.entity.PaymentOutboxEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Write side of the payment result outbox (F-02). Called from within the payment transaction so the
 * outbox row commits atomically with the payment — the saga return path is never lost to a
 * rollback.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentOutboxService {

  private final PaymentOutboxJpaRepository outboxRepository;
  private final ObjectMapper paymentOutboxObjectMapper;

  /** Serialize the payment result and persist a PENDING outbox row in the caller's transaction. */
  public void record(PaymentResultEvent event) {
    try {
      String payload = paymentOutboxObjectMapper.writeValueAsString(event);
      outboxRepository.save(PaymentOutboxEntity.from(event, payload));
      log.debug("Recorded payment outbox entry for order: {}", event.getOrderId());
    } catch (JsonProcessingException e) {
      throw new PaymentOutboxSerializationException("Failed to serialize payment result event", e);
    }
  }
}
