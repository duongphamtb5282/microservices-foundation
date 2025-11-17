package com.pacific.auth.modules.permission.entity;

import com.pacific.core.audit.BaseAuditEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** RolePermission entity for many-to-many relationship between Role and Permission. */
@Entity
@Table(name = "tbl_role_permission", schema = "auth")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission extends BaseAuditEntity {

  @Column(name = "role_id", nullable = false)
  private java.util.UUID roleId;

  @Column(name = "permission_id", nullable = false)
  private java.util.UUID permissionId;

  @Column(name = "assigned_at")
  private LocalDateTime assignedAt;

  @Column(name = "assigned_by")
  private String assignedBy;

  @Column(name = "is_active")
  @lombok.Builder.Default
  private Boolean isActive = true;

  /** Constructor for creating a role permission assignment */
  public RolePermission(java.util.UUID roleId, java.util.UUID permissionId, String assignedBy) {
    this.roleId = roleId;
    this.permissionId = permissionId;
    this.assignedBy = assignedBy;
    this.assignedAt = LocalDateTime.now();
    this.isActive = true;
  }
}
