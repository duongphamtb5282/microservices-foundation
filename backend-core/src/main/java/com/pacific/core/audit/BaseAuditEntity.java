package com.pacific.core.audit;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * Abstract base entity that provides UUID primary key and comprehensive audit fields for entities.
 * This entity includes: - UUID primary key with Hibernate UUID generator - Standard audit fields
 * automatically populated by Spring Data JPA auditing - Version field for optimistic locking - Soft
 * delete support
 *
 * <p>Use this base class for entities that need: - UUID primary keys (better for distributed
 * systems, security, and URL safety) - Comprehensive audit trail functionality - Optimistic locking
 * - Soft delete capability
 *
 * <p>Audit fields: - id: UUID primary key - version: Version field for optimistic locking -
 * createdBy: User who created the record - createdAt: Timestamp when the record was created -
 * modifiedBy: User who last modified the record - modifiedAt: Timestamp when the record was last
 * modified - deletedBy: User who soft-deleted the record - deletedAt: Timestamp when the record was
 * soft-deleted - isDeleted: Soft delete flag
 */
@MappedSuperclass
@EntityListeners(AuditEntityListener.class)
@Getter
@Setter
public abstract class BaseAuditEntity {

  /** UUID primary key. Automatically generated using Hibernate's UUID generator. */
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  /** Version field for optimistic locking. */
  @Version
  @Column(name = "version")
  private Long version = 0L;

  /** User who created this record. Automatically populated by Spring Data JPA auditing. */
  @CreatedBy
  @Column(name = "created_by", updatable = false)
  private String createdBy;

  /**
   * Timestamp when this record was created. Automatically populated by Spring Data JPA auditing.
   */
  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  /** User who last modified this record. Automatically populated by Spring Data JPA auditing. */
  @LastModifiedBy
  @Column(name = "modified_by")
  private String modifiedBy;

  /**
   * Timestamp when this record was last modified. Automatically populated by Spring Data JPA
   * auditing.
   */
  @LastModifiedDate
  @Column(name = "modified_at")
  private Instant modifiedAt;

  /** User who soft-deleted this record. */
  @Column(name = "deleted_by")
  private String deletedBy;

  /** Timestamp when this record was soft-deleted. */
  @Column(name = "deleted_at")
  private Instant deletedAt;

  /** Soft delete flag. When true, the record is considered deleted but not physically removed. */
  @Column(name = "is_deleted", nullable = false)
  private Boolean isDeleted = false;

  /**
   * Gets the unique identifier for this entity.
   *
   * @return the UUID identifier
   */
  public UUID getId() {
    return id;
  }

  /**
   * Marks this entity as soft deleted.
   *
   * @param deletedBy the user performing the deletion
   */
  public void markAsDeleted(String deletedBy) {
    this.deletedBy = deletedBy;
    this.deletedAt = Instant.now();
    this.isDeleted = true;
  }

  /**
   * Checks if this entity is soft deleted.
   *
   * @return true if the entity is soft deleted, false otherwise
   */
  public boolean isDeleted() {
    return Boolean.TRUE.equals(isDeleted);
  }

  /** Restores a soft deleted entity. */
  public void restore() {
    this.deletedBy = null;
    this.deletedAt = null;
    this.isDeleted = false;
  }
}
