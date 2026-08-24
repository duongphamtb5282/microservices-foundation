package com.pacific.payment.modules.payment.service.impl;

import com.pacific.payment.modules.payment.domain.Payment;
import com.pacific.payment.modules.payment.repository.PaymentRepository;
import com.pacific.payment.modules.payment.service.PaymentService;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Implementation of PaymentService */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

  private final PaymentRepository paymentRepository;
  private final Random random = new Random();

  @Override
  public boolean existsByOrderId(String orderId) {
    return paymentRepository.existsByOrderId(orderId);
  }

  @Override
  public boolean processPayment(Payment payment) {
    log.info(
        "Processing payment for order: {}, amount: {}", payment.getOrderId(), payment.getAmount());

    // F-16: the simulated 500ms Thread.sleep was removed — it ran INSIDE the create-payment
    // transaction and held a Hikari connection (pool max 10) for 500ms, capping throughput at
    // ~20 payments/sec per pod. The 90% success-rate simulation below keeps the demo behavior.
    // No try/catch (ADR-0012): the simulation cannot throw — a domain exception would roll back the
    // create-payment transaction for the saga to re-deliver, and must not be masked as a business
    // "gateway error" outcome.
    boolean success = random.nextInt(10) < 9;

    if (success) {
      String transactionId = "TXN-" + UUID.randomUUID().toString();
      payment.complete(transactionId, "Payment processed successfully");
      log.info(
          "Payment successful: orderId={}, transactionId={}",
          payment.getOrderId(),
          transactionId);
      return true;
    } else {
      payment.fail("Insufficient funds or card declined");
      log.warn("Payment failed: orderId={}", payment.getOrderId());
      return false;
    }
  }
}
