package com.pacific.auth.modules.role.entity;

import com.pacific.auth.modules.permission.entity.Permission;
import com.pacific.auth.modules.user.entity.User;
import com.pacific.core.audit.BaseAuditEntity;
import jakarta.persistence.*;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** Role entity for role-based access control. Represents user roles in the system. */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(
    name = "tbl_role",
    schema = "auth",
    uniqueConstraints = {@UniqueConstraint(columnNames = "name", name = "uk_roles_name")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Role extends BaseAuditEntity {
  @Enumerated(EnumType.STRING)
  @Column(name = "name", nullable = false, unique = true, length = 50)
  private RoleType name;

  @Column(name = "description", length = 255)
  private String description;

  // Many-to-many relationship with User
  @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Set<User> users;

  // Many-to-many relationship with Permission
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "tbl_role_permission",
      joinColumns = @JoinColumn(name = "role_id"),
      inverseJoinColumns = @JoinColumn(name = "permission_id"))
  @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"roles"})
  private Set<Permission> permissions;

  /** Constructor for creating a role with name and description */
  public Role(RoleType name, String description) {
    this.name = name;
    this.description = description;
  }

  /** Constructor for creating a role with just the name (uses default description) */
  public Role(RoleType name) {
    this.name = name;
    this.description = name.getDescription();
  }

  /** Get role name in Spring Security format (ROLE_*) */
  public String getAuthority() {
    return name.getAuthority();
  }
}
