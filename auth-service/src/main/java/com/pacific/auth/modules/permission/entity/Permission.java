package com.pacific.auth.modules.permission.entity;

import com.pacific.auth.modules.role.entity.Role;
import com.pacific.core.audit.BaseAuditEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.*;

/** Permission entity for role-based access control. */
@Entity
@Table(name = "tbl_permission", schema = "auth")
@org.hibernate.annotations.Cache(
    usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission extends BaseAuditEntity {

  @NotBlank
  @Size(max = 50)
  @Column(name = "name", length = 50, unique = true, nullable = false)
  private String name;

  @Size(max = 255)
  @Column(name = "description")
  private String description;

  @Size(max = 100)
  @Column(name = "resource", length = 100)
  private String resource;

  @Size(max = 50)
  @Column(name = "action", length = 50)
  private String action;

  // Many-to-many relationship with Role
  @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Set<Role> roles;

  /** Constructor for creating a permission with name and description */
  public Permission(String name, String description) {
    this.name = name;
    this.description = description;
  }

  /** Constructor for creating a permission with name, description, resource, and action */
  public Permission(String name, String description, String resource, String action) {
    this.name = name;
    this.description = description;
    this.resource = resource;
    this.action = action;
  }
}
