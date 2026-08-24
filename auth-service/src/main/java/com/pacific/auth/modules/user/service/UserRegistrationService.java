package com.pacific.auth.modules.user.service;

import com.pacific.auth.modules.authentication.client.dto.KeycloakCredentialRepresentation;
import com.pacific.auth.modules.authentication.client.dto.KeycloakUserRepresentation;
import com.pacific.auth.modules.authentication.service.KeycloakService;
import com.pacific.auth.modules.outbox.service.UserOutboxService;
import com.pacific.auth.modules.user.dto.request.RegistrationRequestDto;
import com.pacific.auth.modules.user.dto.response.RegistrationResponseDto;
import com.pacific.core.filter.CorrelationIdFilter;
import com.pacific.shared.events.UserCreatedEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * User registration — thin Keycloak proxy. The local DB user store was removed with the dual-auth
 * stack (S-06): registering creates the user in Keycloak, which is the single user store, and
 * Keycloak's realm default roles apply on account creation. The transactional outbox write is kept
 * (ADR-0006): each successful Keycloak user creation enqueues a UserCreatedEvent so downstream
 * services (ms-customer's UserEventConsumer) still provision customers — the event ledger is not a
 * user store.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService {

  private final KeycloakService keycloakService;
  private final UserOutboxService userOutboxService;

  /** Register a new user in Keycloak (admin API), then enqueue the UserCreatedEvent (ADR-0006). */
  public RegistrationResponseDto registerUser(RegistrationRequestDto request) {
    log.info("🚀 Registering user in Keycloak: {}", request.getUsername());

    KeycloakUserRepresentation user = new KeycloakUserRepresentation();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setFirstName(request.getFirstName());
    user.setLastName(request.getLastName());
    user.setEnabled(true);
    user.setEmailVerified(false);

    KeycloakCredentialRepresentation credential = new KeycloakCredentialRepresentation();
    credential.setType("password");
    credential.setValue(request.getPassword());
    credential.setTemporary(false);
    user.setCredentials(List.of(credential));

    Map<String, List<String>> attributes = new HashMap<>();
    if (request.getPhoneNumber() != null) {
      attributes.put("phoneNumber", List.of(request.getPhoneNumber()));
    }
    if (request.getAddress() != null) {
      attributes.put("address", List.of(request.getAddress()));
    }
    if (!attributes.isEmpty()) {
      user.setAttributes(attributes);
    }

    keycloakService.createUser(user);
    log.info("✅ User registered in Keycloak: {}", request.getUsername());

    // Outbox enqueue (ADR-0006) — only after Keycloak accepts the user, so a failed registration
    // never emits an event. userId is the Keycloak username: createUser returns void (the admin
    // API's Location header is discarded), and the consumer treats the id as opaque, deduplicating
    // on email.
    UserCreatedEvent event =
        UserCreatedEvent.builder()
            .userId(request.getUsername())
            .username(request.getUsername())
            .email(request.getEmail())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phone(request.getPhoneNumber())
            .build();
    // Chain the request correlation id (X-Correlation-ID → MDC via CorrelationIdFilter) so the
    // consumer's trace links back; falls back to the event's fresh UUID.
    String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
    if (correlationId != null && !correlationId.isBlank() && !"unknown".equals(correlationId)) {
      event = event.withCorrelationId(correlationId);
    }
    userOutboxService.record(event);

    return new RegistrationResponseDto(request.getUsername(), request.getEmail());
  }
}
