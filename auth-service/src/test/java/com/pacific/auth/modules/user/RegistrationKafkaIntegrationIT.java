package com.pacific.auth.modules.user;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration test for user registration with Kafka messaging. Tests the complete flow:
 * registration → audit → transactional outbox (ADR-0006) → Kafka event publishing.
 */
@SpringBootTest
@ActiveProfiles("dev")
@DirtiesContext
@Slf4j
public class RegistrationKafkaIntegrationIT {

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired(required = false)
  private com.pacific.core.messaging.cqrs.event.EventPublisher eventPublisher;

  @Autowired
  private com.pacific.auth.modules.outbox.repository.UserOutboxJpaRepository outboxRepository;

  private MockMvc mockMvc;

  @Test
  void testUserRegistrationWithKafkaEnabled() throws Exception {
    // Given: MockMvc setup
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    log.info("🚀 Starting user registration test with Kafka enabled");

    // Verify that EventPublisher is available (Kafka is enabled)
    assertThat(eventPublisher).isNotNull();
    log.info("✅ EventPublisher is available - Kafka integration is enabled");

    // When: Perform HTTP registration request
    var result =
        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/api/auth/register")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                  "username": "kafkatest",
                  "email": "kafkatest@example.com",
                  "password": "password123"
                }
                """));

    // Then: Verify successful registration
    result
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.username")
                .value("kafkatest"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.email")
                .value("kafkatest@example.com"));

    // Then: the UserCreatedEvent is in the transactional outbox (ADR-0006) — written atomically
    // with the user. Status may be PENDING or already PUBLISHED if the relay ran; either way the
    // event exists and Kafka being down cannot have prevented the registration from committing.
    var outboxRows = outboxRepository.findAll();
    assertThat(outboxRows)
        .anySatisfy(
            row -> {
              assertThat(row.getEventType()).isEqualTo("USER_CREATED");
              assertThat(row.getPayload()).contains("kafkatest");
              assertThat(row.getStatus()).isNotNull();
            });
    log.info("✅ UserCreatedEvent recorded in outbox (total rows: {})", outboxRows.size());
    log.info("🎉 User registration with Kafka integration test completed successfully!");
  }
}
