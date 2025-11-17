package com.pacific.auth.modules.user.service;

import com.pacific.auth.common.exception.UserAlreadyExistsException;
import com.pacific.auth.modules.role.entity.Role;
import com.pacific.auth.modules.role.entity.RoleType;
import com.pacific.auth.modules.role.service.RoleService;
import com.pacific.auth.modules.user.entity.User;
import com.pacific.auth.modules.user.repository.UserRepository;
import com.pacific.core.audit.Audit;
import com.pacific.core.audit.AuditAction;
import com.pacific.core.messaging.cqrs.event.EventPublisher;
import com.pacific.shared.events.UserCreatedEvent;
import com.pacific.shared.utils.ValidationUtils;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final RoleService roleService;

  @Autowired(required = false)
  private EventPublisher eventPublisher;

  @Transactional
  @Audit(entityType = "User", action = AuditAction.CREATED, details = "User registration completed")
  public User registerUser(User user) {
    try {
      // Validate input using shared utilities
      ValidationUtils.validateEmail(user.getEmail());
      ValidationUtils.validateUsername(user.getUserName());
      ValidationUtils.validatePassword(user.getPassword());

      // Check for duplicates
      boolean emailExists = userRepository.existsByEmail(user.getEmail());
      boolean usernameExists = userRepository.existsByUserName(user.getUserName());

      if (emailExists && usernameExists) {
        throw UserAlreadyExistsException.forBoth(user.getEmail(), user.getUserName());
      } else if (emailExists) {
        throw UserAlreadyExistsException.forEmail(user.getEmail());
      } else if (usernameExists) {
        throw UserAlreadyExistsException.forUsername(user.getUserName());
      }

      // Create new User object with audit fields set
      User newUser =
          new User(user.getUserName(), user.getEmail(), passwordEncoder.encode(user.getPassword()));

      // Copy additional fields from input
      newUser.setFirstName(user.getFirstName());
      newUser.setLastName(user.getLastName());
      newUser.setPhoneNumber(user.getPhoneNumber());
      newUser.setAddress(user.getAddress());

      // Assign default USER role
      Role userRole = roleService.getOrCreateRole(RoleType.USER);
      newUser.setRoles(Set.of(userRole));

      log.info("Assigning default USER role to new user: {}", newUser.getUserName());
      log.info("User roles set: {}", newUser.getRoles());
      log.info("About to save user to database: {}", newUser.getUserName());

      log.info("About to call userRepository.save() for user: {}", newUser.getUserName());
      User savedUser = userRepository.save(newUser);
      log.info("✅ User successfully saved to database: {}", savedUser.getUserName());

      // Publish UserCreatedEvent to Kafka using backend-core EventPublisher
      UserCreatedEvent userCreatedEvent =
          new UserCreatedEvent(
              savedUser.getId().toString(), savedUser.getUserName(), savedUser.getEmail());

      if (eventPublisher != null) {
        try {
          log.info("📨 Publishing UserCreatedEvent to Kafka topic 'user-events'");
          eventPublisher
              .publish("user-events", savedUser.getId().toString(), userCreatedEvent)
              .whenComplete(
                  (result, ex) -> {
                    if (ex == null) {
                      log.info(
                          "✅ UserCreatedEvent published successfully to Kafka - Partition: {}, Offset: {}",
                          result.getRecordMetadata().partition(),
                          result.getRecordMetadata().offset());
                    } else {
                      log.error("❌ Failed to publish UserCreatedEvent to Kafka", ex);
                    }
                  });
        } catch (Exception e) {
          log.warn("⚠️  Kafka not available, continuing with registration: {}", e.getMessage());
        }
      } else {
        log.info("📨 EventPublisher not available - skipping Kafka event publishing");
      }

      return savedUser;
    } catch (Exception e) {
      log.error("❌ Registration failed for user: {}", user.getUserName(), e);
      log.error("Exception type: {}", e.getClass().getSimpleName());
      log.error("Exception message: {}", e.getMessage());
      if (e.getCause() != null) {
        log.error("Caused by: {}", e.getCause().getMessage());
      }
      throw e;
    }
  }
}
