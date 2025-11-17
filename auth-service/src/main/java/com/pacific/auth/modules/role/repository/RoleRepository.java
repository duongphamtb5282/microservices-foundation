package com.pacific.auth.modules.role.repository;

import com.pacific.auth.modules.role.entity.Role;
import com.pacific.auth.modules.role.entity.RoleType;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for Role entity operations. */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

  /**
   * Find role by name
   *
   * @param name the role name
   * @return Optional containing the role if found
   */
  Optional<Role> findByName(RoleType name);

  /**
   * Check if role exists by name
   *
   * @param name the role name
   * @return true if role exists
   */
  boolean existsByName(RoleType name);

  /**
   * Find roles by user ID
   *
   * @param userId the user ID
   * @return Set of roles for the user
   */
  @Query("SELECT r FROM Role r JOIN r.users u WHERE u.id = :userId")
  Set<Role> findByUserId(@Param("userId") UUID userId);

  /**
   * Find roles by user email
   *
   * @param email the user email
   * @return Set of roles for the user
   */
  @Query("SELECT r FROM Role r JOIN r.users u WHERE u.email = :email")
  Set<Role> findByUserEmail(@Param("email") String email);
}
