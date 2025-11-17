package com.pacific.payment.modules.payment.repository;

import com.pacific.payment.modules.payment.domain.Payment;
import com.pacific.payment.modules.payment.entity.PaymentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementing PaymentRepository interface
 */
@Component
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = toEntity(payment);
        PaymentEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Payment> findById(String id) {
        return jpaRepository.findById(id)
            .map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByOrderId(String orderId) {
        return jpaRepository.findByOrderId(orderId)
            .map(this::toDomain);
    }

    @Override
    public List<Payment> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByOrderId(String orderId) {
        return jpaRepository.existsByOrderId(orderId);
    }

    @Override
    public List<Payment> findAll() {
        return jpaRepository.findAll().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Convert domain Payment to PaymentEntity
     */
    private PaymentEntity toEntity(Payment payment) {
        return PaymentEntity.builder()
            .id(payment.getId())
            .orderId(payment.getOrderId())
            .userId(payment.getUserId())
            .amount(payment.getAmount())
            .method(payment.getMethod())
            .status(payment.getStatus())
            .gatewayTransactionId(payment.getGatewayTransactionId())
            .gatewayResponse(payment.getGatewayResponse())
            .createdAt(payment.getCreatedAt())
            .updatedAt(payment.getUpdatedAt())
            .createdBy(payment.getCreatedBy())
            .updatedBy(payment.getUpdatedBy())
            .version(payment.getVersion())
            .build();
    }

    /**
     * Convert PaymentEntity to domain Payment
     */
    private Payment toDomain(PaymentEntity entity) {
        return Payment.builder()
            .id(entity.getId())
            .orderId(entity.getOrderId())
            .userId(entity.getUserId())
            .amount(entity.getAmount())
            .method(entity.getMethod())
            .status(entity.getStatus())
            .gatewayTransactionId(entity.getGatewayTransactionId())
            .gatewayResponse(entity.getGatewayResponse())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .version(entity.getVersion())
            .build();
    }
}

