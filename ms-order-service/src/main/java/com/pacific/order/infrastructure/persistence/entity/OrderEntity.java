package com.pacific.order.infrastructure.persistence.entity;

import com.pacific.order.domain.model.OrderStatus;
import jakarta.persistence.*;
import com.pacific.order.infrastructure.security.EncryptedString;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity for Order table
 */
@Entity
@Table(name = "orders")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private OrderStatus status;

    // Customer information (encrypted)
    @Column(name = "customer_email", length = 500)
    @Convert(converter = EncryptedString.class)
    private String customerEmail;

    @Column(name = "customer_phone", length = 500)
    @Convert(converter = EncryptedString.class)
    private String customerPhone;

    @Column(name = "shipping_address", length = 1000)
    @Convert(converter = EncryptedString.class)
    private String shippingAddress;

    // Security fields
    @Column(name = "data_encryption_key_id", length = 100)
    private String dataEncryptionKeyId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItemEntity> items = new ArrayList<>();

    // Audit fields
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100, nullable = false)
    private String createdBy;

    @Column(name = "updated_by", length = 100, nullable = false)
    private String updatedBy;

    @Version
    @Column(name = "version")
    private Integer version;

    /**
     * Helper method to add items to the order
     */
    public void addItem(OrderItemEntity item) {
        items.add(item);
        item.setOrder(this);
    }

    /**
     * Helper method to remove items from the order
     */
    public void removeItem(OrderItemEntity item) {
        items.remove(item);
        item.setOrder(null);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

