package com.pacific.auth.modules.user.service;

import com.pacific.auth.common.exception.UserNotFoundException;
import com.pacific.auth.modules.user.dto.response.UserInfoDto;
import com.pacific.auth.modules.user.entity.User;
import com.pacific.auth.modules.user.repository.UserRepository;
import com.pacific.core.service.CacheService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** User service with advanced caching strategies and SQL optimization */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;

  @Autowired(required = false)
  private CacheService cacheService;

  /**
   * Get current user information with multi-layer caching Caching is done at service layer to cache
   * the DTO, not the ResponseEntity
   */
  @Transactional(readOnly = true)
  @Cacheable(value = "user-info", key = "#username")
  public UserInfoDto getCurrentUserInfo(String username) {
    log.info("🔍 Fetching user info from database for: {}", username);

    User user =
        userRepository
            .findByUserName(username)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

    return mapToUserInfoDto(user);
  }

  /** Get user by ID with caching */
  @Transactional(readOnly = true)
  @org.springframework.cache.annotation.Cacheable(value = "user-by-id", key = "#userId")
  public Optional<UserInfoDto> getUserById(String userId) {
    log.info("🔍 Fetching user by ID from database: {}", userId);

    try {
      UUID userUuid = UUID.fromString(userId);
      Optional<User> user = userRepository.findById(userUuid);
      return user.map(this::mapToUserInfoDto);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid UUID format: {}", userId);
      return Optional.empty();
    }
  }

  /** Get user by username with caching */
  @Transactional(readOnly = true)
  @Cacheable(value = "user-by-username", key = "#username")
  public Optional<UserInfoDto> getUserByUsername(String username) {
    log.info("🔍 Fetching user by username from database: {}", username);

    Optional<User> user = userRepository.findByUserName(username);
    return user.map(this::mapToUserInfoDto);
  }

  /** Get all users with pagination and caching */
  public Map<String, Object> getAllUsers(int page, int size) {
    log.info("🔍 Fetching all users from database - page: {}, size: {}", page, size);

    Pageable pageable = PageRequest.of(page, size);
    Page<User> userPage = userRepository.findAll(pageable);

    List<UserInfoDto> users =
        userPage.getContent().stream().map(this::mapToUserInfoDto).collect(Collectors.toList());

    Map<String, Object> result = new HashMap<>();
    result.put("users", users);
    result.put("totalElements", userPage.getTotalElements());
    result.put("totalPages", userPage.getTotalPages());
    result.put("currentPage", page);
    result.put("size", size);
    result.put("first", userPage.isFirst());
    result.put("last", userPage.isLast());

    return result;
  }

  /** Search users with caching */
  @Cacheable(value = "user-search", key = "#query + '-' + #page + '-' + #size")
  public Map<String, Object> searchUsers(String query, int page, int size) {
    log.info(
        "🔍 Searching users in database with query: '{}', page: {}, size: {}", query, page, size);

    Pageable pageable = PageRequest.of(page, size);
    Page<User> userPage =
        userRepository.findByUserNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            query, query, pageable);

    List<UserInfoDto> users =
        userPage.getContent().stream().map(this::mapToUserInfoDto).collect(Collectors.toList());

    Map<String, Object> result = new HashMap<>();
    result.put("users", users);
    result.put("totalElements", userPage.getTotalElements());
    result.put("totalPages", userPage.getTotalPages());
    result.put("currentPage", page);
    result.put("size", size);
    result.put("query", query);
    result.put("first", userPage.isFirst());
    result.put("last", userPage.isLast());

    return result;
  }

  /** Update user with cache invalidation */
  @Transactional
  @CachePut(value = "user-by-id", key = "#userId")
  @CacheEvict(
      value = {"user-info", "user-by-username", "all-users", "user-search"},
      allEntries = true)
  public UserInfoDto updateUser(String userId, Map<String, Object> updates) {
    log.info("🔄 Updating user in database: {} with updates: {}", userId, updates);

    UUID userUuid = UUID.fromString(userId);
    User user =
        userRepository
            .findById(userUuid)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

    // Apply updates
    updates.forEach(
        (key, value) -> {
          switch (key) {
            case "firstName" -> user.setFirstName((String) value);
            case "lastName" -> user.setLastName((String) value);
            case "email" -> user.setEmail((String) value);
            case "phoneNumber" -> user.setPhoneNumber((String) value);
            case "address" -> user.setAddress((String) value);
            case "isActive" -> user.setIsActive((Boolean) value);
            default -> log.warn("Unknown field for update: {}", key);
          }
        });

    User updatedUser = userRepository.save(user);
    log.info("✅ User updated successfully: {}", userId);

    return mapToUserInfoDto(updatedUser);
  }

  /** Delete user with cache invalidation */
  @Transactional
  @CacheEvict(
      value = {
        "user-info",
        "user-by-id",
        "user-by-username",
        "all-users",
        "user-search",
        "user-roles"
      },
      allEntries = true)
  public void deleteUser(String userId) {
    log.info("🗑️ Deleting user from database: {}", userId);

    UUID userUuid = UUID.fromString(userId);
    if (!userRepository.existsById(userUuid)) {
      throw new RuntimeException("User not found: " + userId);
    }

    userRepository.deleteById(userUuid);
    log.info("✅ User deleted successfully: {}", userId);
  }

  /** Get user roles with caching */
  @Cacheable(value = "user-roles", key = "#userId")
  public Map<String, Object> getUserRoles(String userId) {
    log.info("🔍 Fetching user roles from database for user: {}", userId);

    UUID userUuid = UUID.fromString(userId);
    User user =
        userRepository
            .findById(userUuid)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

    List<String> roles =
        user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toList());

    Map<String, Object> result = new HashMap<>();
    result.put("userId", userId);
    result.put("username", user.getUserName());
    result.put("roles", roles);
    result.put("roleCount", roles.size());

    return result;
  }

  /** Get user statistics with caching */
  @Cacheable(value = "user-stats", key = "'stats'")
  public Map<String, Object> getUserStatistics() {
    log.info("📊 Fetching user statistics from database");

    long totalUsers = userRepository.count();
    long activeUsers = userRepository.countByIsActiveTrue();
    long inactiveUsers = userRepository.countByIsActiveFalse();

    Map<String, Object> stats = new HashMap<>();
    stats.put("totalUsers", totalUsers);
    stats.put("activeUsers", activeUsers);
    stats.put("inactiveUsers", inactiveUsers);
    stats.put("activePercentage", totalUsers > 0 ? (double) activeUsers / totalUsers * 100 : 0);

    return stats;
  }

  /** Get users by role with caching */
  @Cacheable(value = "users-by-role", key = "#roleName + '-' + #page + '-' + #size")
  public Map<String, Object> getUsersByRole(String roleName, int page, int size) {
    log.info(
        "🔍 Fetching users by role from database: {}, page: {}, size: {}", roleName, page, size);

    Pageable pageable = PageRequest.of(page, size);
    Page<User> userPage = userRepository.findByRolesName(roleName, pageable);

    List<UserInfoDto> users =
        userPage.getContent().stream().map(this::mapToUserInfoDto).collect(Collectors.toList());

    Map<String, Object> result = new HashMap<>();
    result.put("users", users);
    result.put("totalElements", userPage.getTotalElements());
    result.put("totalPages", userPage.getTotalPages());
    result.put("currentPage", page);
    result.put("size", size);
    result.put("roleName", roleName);
    result.put("first", userPage.isFirst());
    result.put("last", userPage.isLast());

    return result;
  }

  /** Map User entity to UserInfoDto */
  private UserInfoDto mapToUserInfoDto(User user) {
    return UserInfoDto.builder()
        .id(user.getId() != null ? user.getId().toString() : null)
        .userName(user.getUserName())
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .phoneNumber(user.getPhoneNumber())
        .address(user.getAddress())
        .isActive(user.getIsActive())
        .createdAt(convertToInstant(user.getCreatedAt()))
        .modifiedAt(convertToInstant(user.getModifiedAt()))
        .roles(
            user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList()))
        .build();
  }

  /** Clear all user-related caches */
  public void clearUserCaches() {
    log.info("🧹 Clearing all user-related caches");

    cacheService.clear("user-info");
    cacheService.clear("user-by-id");
    cacheService.clear("user-by-username");
    cacheService.clear("all-users");
    cacheService.clear("user-search");
    cacheService.clear("user-roles");
    cacheService.clear("user-stats");
    cacheService.clear("users-by-role");

    log.info("✅ All user-related caches cleared");
  }

  /** Warm up caches for frequently accessed users */
  public void warmUpCaches() {
    log.info("🔥 Warming up user caches");

    try {
      // Warm up user statistics
      getUserStatistics();

      // Warm up first page of users
      getAllUsers(0, 10);

      log.info("✅ User caches warmed up successfully");
    } catch (Exception e) {
      log.error("❌ Error warming up user caches", e);
    }
  }

  private java.time.Instant convertToInstant(Object dateTime) {
    if (dateTime == null) {
      return null;
    }
    if (dateTime instanceof java.time.Instant) {
      return (java.time.Instant) dateTime;
    }
    if (dateTime instanceof java.time.LocalDateTime) {
      return ((java.time.LocalDateTime) dateTime)
          .atZone(java.time.ZoneId.systemDefault())
          .toInstant();
    }
    return null;
  }
}
