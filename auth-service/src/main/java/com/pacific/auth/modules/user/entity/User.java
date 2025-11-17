package com.pacific.auth.modules.user.entity;

import com.pacific.auth.modules.role.entity.Role;
import com.pacific.core.audit.BaseAuditEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** A user. */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "tbl_user", schema = "auth")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseAuditEntity {

  @Serial private static final long serialVersionUID = 1L;

  @Size(min = 5, max = 20)
  @Column(name = "user_name", length = 20)
  private String userName;

  @Email
  @Size(min = 5, max = 254)
  @Column(length = 254, unique = true)
  private String email;

  @Size(max = 100)
  @Column(name = "password_hash", length = 100)
  private String password;

  @Size(max = 50)
  @Column(name = "first_name", length = 50)
  private String firstName;

  @Size(max = 50)
  @Column(name = "last_name", length = 50)
  private String lastName;

  @Size(max = 20)
  @Column(name = "phone_number", length = 20)
  private String phoneNumber;

  @Column(name = "address", columnDefinition = "TEXT")
  private String address;

  @Builder.Default
  @Column(name = "is_active")
  private Boolean isActive = true;

  // Many-to-many relationship with Role

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "tbl_user_role",
      schema = "auth",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"users"})
  private Set<Role> roles;

  // Custom constructor to set audit fields for registration
  public User(String username, String email, String passwordHash) {
    this.userName = username;
    this.email = email;
    this.password = passwordHash;
    this.isActive = true;

    // Manually set audit fields since JPA auditing is not working
    this.setCreatedBy("system");
    this.setCreatedAt(java.time.Instant.now());
  }
}
