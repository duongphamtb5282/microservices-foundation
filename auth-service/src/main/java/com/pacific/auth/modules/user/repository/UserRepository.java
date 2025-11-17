package com.pacific.auth.modules.user.repository;

import com.pacific.auth.modules.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for the User entity with advanced query methods. */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  // Basic finder methods
  Optional<User> findByUserName(String userName);

  boolean existsByUserName(String userName);

  boolean existsByEmail(String email);

  // Count methods for statistics
  long countByIsActiveTrue();

  long countByIsActiveFalse();

  // Search methods with pagination
  Page<User> findByUserNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
      String userName, String email, Pageable pageable);

  Page<User> findByRolesName(String roleName, Pageable pageable);

  // Advanced query methods
  @Query("SELECT u FROM User u WHERE u.userName = :username")
  Optional<User> findByUserNameWithoutRoles(@Param("username") String username);

  @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.userName = :username")
  Optional<User> findByUserNameWithRoles(@Param("username") String username);

  @Query("SELECT u FROM User u WHERE u.isActive = :isActive")
  Page<User> findByIsActive(@Param("isActive") Boolean isActive, Pageable pageable);

  @Query("SELECT u FROM User u WHERE u.firstName LIKE %:name% OR u.lastName LIKE %:name%")
  Page<User> findByNameContaining(@Param("name") String name, Pageable pageable);

  @Query("SELECT u FROM User u WHERE u.email LIKE %:email%")
  Page<User> findByEmailContaining(@Param("email") String email, Pageable pageable);

  // Statistics queries
  @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
  long countActiveUsers();

  @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = false")
  long countInactiveUsers();

  @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
  long countByRoleName(@Param("roleName") String roleName);
}
