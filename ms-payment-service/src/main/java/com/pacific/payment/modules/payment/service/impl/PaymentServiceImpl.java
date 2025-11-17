package com.pacific.payment.modules.payment.service.impl;

import com.pacific.payment.modules.payment.domain.Payment;
import com.pacific.payment.modules.payment.repository.PaymentRepository;
import com.pacific.payment.modules.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

/**
 * Implementation of PaymentService
 */
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
        log.info("Processing payment for order: {}, amount: {}",
                payment.getOrderId(), payment.getAmount());

        try {
            // Simulate payment gateway call
            Thread.sleep(500); // Simulate network latency

            // Simulate 90% success rate
            boolean success = random.nextInt(10) < 9;

            if (success) {
                String transactionId = "TXN-" + UUID.randomUUID().toString();
                payment.complete(transactionId, "Payment processed successfully");
                log.info("Payment successful: orderId={}, transactionId={}",
                        payment.getOrderId(), transactionId);
                return true;
            } else {
                payment.fail("Insufficient funds or card declined");
                log.warn("Payment failed: orderId={}", payment.getOrderId());
                return false;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Payment processing interrupted", e);
            payment.fail("Payment processing interrupted");
            return false;
        } catch (Exception e) {
            log.error("Error processing payment", e);
            payment.fail("Payment gateway error: " + e.getMessage());
            return false;
        }
    }
}

