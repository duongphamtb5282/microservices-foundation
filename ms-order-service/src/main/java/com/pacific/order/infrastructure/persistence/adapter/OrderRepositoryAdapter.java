package com.pacific.order.infrastructure.persistence.adapter;

import com.pacific.core.messaging.security.SecurityService;
import com.pacific.order.domain.model.Money;
import com.pacific.order.domain.model.Order;
import com.pacific.order.domain.model.OrderItem;
import com.pacific.order.domain.repository.OrderRepository;
import com.pacific.order.infrastructure.persistence.entity.OrderEntity;
import com.pacific.order.infrastructure.persistence.entity.OrderItemEntity;
import com.pacific.order.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementing OrderRepository interface
 * Translates between domain models and JPA entities
 */
@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;
    private final SecurityService securityService;

    @Override
    public Order save(Order order) {
        OrderEntity entity = toEntity(order);
        OrderEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Order> findById(String id) {
        return jpaRepository.findById(id)
            .map(this::toDomain);
    }

    @Override
    public List<Order> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<Order> findAll() {
        return jpaRepository.findAll().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Convert domain Order to OrderEntity
     */
    private OrderEntity toEntity(Order order) {
        OrderEntity entity = OrderEntity.builder()
            .id(order.getId())
            .userId(order.getUserId())
            .totalAmount(order.getTotalAmount().getAmount())
            .currency(order.getTotalAmount().getCurrency().getCurrencyCode())
            .status(order.getStatus())
            .customerEmail(encryptIfNotNull(order.getCustomerEmail()))
            .customerPhone(encryptIfNotNull(order.getCustomerPhone()))
            .shippingAddress(encryptIfNotNull(order.getShippingAddress()))
            .dataEncryptionKeyId(order.getDataEncryptionKeyId())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .createdBy(order.getCreatedBy())
            .updatedBy(order.getUpdatedBy())
            .version(order.getVersion())
            .build();

        // Convert items
        if (order.getItems() != null) {
            List<OrderItemEntity> itemEntities = order.getItems().stream()
                .map(item -> toItemEntity(item, entity))
                .collect(Collectors.toList());
            entity.setItems(itemEntities);
        }

        return entity;
    }

    /**
     * Convert OrderEntity to domain Order
     */
    private Order toDomain(OrderEntity entity) {
        Money totalAmount = new Money(
            entity.getTotalAmount(),
            Currency.getInstance(entity.getCurrency())
        );

        List<OrderItem> items = entity.getItems().stream()
            .map(this::toItemDomain)
            .collect(Collectors.toList());

        return Order.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .items(items)
            .totalAmount(totalAmount)
            .status(entity.getStatus())
            .customerEmail(entity.getCustomerEmail())
            .customerPhone(entity.getCustomerPhone())
            .shippingAddress(entity.getShippingAddress())
            .dataEncryptionKeyId(entity.getDataEncryptionKeyId())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .version(entity.getVersion())
            .build();
    }

    /**
     * Convert domain OrderItem to OrderItemEntity
     */
    private OrderItemEntity toItemEntity(OrderItem item, OrderEntity order) {
        return OrderItemEntity.builder()
            .id(item.getId())
            .order(order)
            .productName(item.getProductName())
            .description(item.getDescription())
            .quantity(item.getQuantity())
            .unitPrice(item.getUnitPrice().getAmount())
            .totalPrice(item.getTotalPrice().getAmount())
            .createdAt(java.time.LocalDateTime.now())
            .build();
    }

    /**
     * Convert OrderItemEntity to domain OrderItem
     */
    private OrderItem toItemDomain(OrderItemEntity entity) {
        Money unitPrice = Money.usd(entity.getUnitPrice());
        Money totalPrice = Money.usd(entity.getTotalPrice());

        return OrderItem.builder()
            .id(entity.getId())
            .orderId(entity.getOrder().getId())
            .productName(entity.getProductName())
            .description(entity.getDescription())
            .quantity(entity.getQuantity())
            .unitPrice(unitPrice)
            .totalPrice(totalPrice)
            .build();
    }

    /**
     * Encrypt string if not null.
     */
    private String encryptIfNotNull(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return securityService.encrypt(value);
    }
}

