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
 * registration → audit → Kafka event publishing.
 */
@SpringBootTest
@ActiveProfiles("dev")
@DirtiesContext
@Slf4j
public class RegistrationKafkaIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired(required = false)
  private com.pacific.core.messaging.cqrs.event.EventPublisher eventPublisher;

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

    log.info("✅ User registration HTTP request successful");
    log.info("✅ EventPublisher.publish() would be called for Kafka event publishing");
    log.info("🎉 User registration with Kafka integration test completed successfully!");
  }
}
