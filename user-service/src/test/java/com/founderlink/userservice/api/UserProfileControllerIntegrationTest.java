package com.founderlink.userservice.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class UserProfileControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
    jdbcTemplate.execute("DELETE FROM user_profiles");
    jdbcTemplate.execute("DELETE FROM admin");
    jdbcTemplate.execute("DELETE FROM founder");
    jdbcTemplate.execute("DELETE FROM investor");
    jdbcTemplate.execute("DELETE FROM co_founder");
    jdbcTemplate.execute("DELETE FROM users");
    jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
  }

  @Test
  @WithMockUser(
      username = "1",
      roles = {"FOUNDER"})
  void shouldCreateFetchListAndUpdateUserProfile() throws Exception {
    String createRequest =
        """
                {
                  "name": "Ava Founder",
                  "email": "ava@founderlink.com",
                  "role": "founder",
                  "skills": "Product, Growth",
                  "experience": "Built 2 SaaS products",
                  "bio": "Founder building in fintech",
                  "portfolioLinks": "https://ava.dev"
                }
                """;

    mockMvc
        .perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(createRequest))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/users/1"))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value("ava@founderlink.com"))
        .andExpect(jsonPath("$.role").value("FOUNDER"));

    mockMvc
        .perform(get("/users/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Ava Founder"));

    mockMvc.perform(get("/users")).andExpect(status().isForbidden());

    String updateRequest =
        """
                {
                  "name": "Ava Founder",
                  "email": "ava.updated@founderlink.com",
                  "role": "founder",
                  "skills": "Product, Growth, Fundraising",
                  "experience": "Built 3 SaaS products",
                  "bio": "Founder building in climate tech",
                  "portfolioLinks": "https://ava.dev/profile"
                }
                """;

    mockMvc
        .perform(put("/users/1").contentType(MediaType.APPLICATION_JSON).content(updateRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("ava.updated@founderlink.com"))
        .andExpect(jsonPath("$.skills").value("Product, Growth, Fundraising"));
  }

  @Test
  void shouldRejectDuplicateEmail() throws Exception {
    String createRequest =
        """
                {
                  "name": "Ava Founder",
                  "email": "duplicate@founderlink.com",
                  "role": "founder"
                }
                """;

    mockMvc
        .perform(
            post("/users")
                .with(SecurityMockMvcRequestPostProcessors.user("1").roles("FOUNDER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/users")
                .with(SecurityMockMvcRequestPostProcessors.user("2").roles("FOUNDER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.message")
                .value("A user profile already exists for email duplicate@founderlink.com"));
  }

  @Test
  @WithMockUser(
      username = "999",
      roles = {"ADMIN"})
  void shouldReturnNotFoundForMissingUser() throws Exception {
    mockMvc
        .perform(get("/users/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("User profile not found for id 999"));
  }

  @Test
  @WithMockUser(
      username = "1",
      roles = {"FOUNDER"})
  void shouldValidateCreateRequest() throws Exception {
    String invalidRequest =
        """
                {
                  "name": "",
                  "email": "not-an-email",
                  "role": ""
                }
                """;

    mockMvc
        .perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(invalidRequest))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details.length()").value(3));
  }

  @Test
  @WithMockUser(
      username = "1",
      roles = {"FOUNDER"})
  void shouldCreateCoFounderProfileUsingFriendlyRoleValue() throws Exception {
    String createRequest =
        """
                {
                  "name": "Noah CoFounder",
                  "email": "noah@founderlink.com",
                  "role": "cofounder",
                  "skills": "Engineering"
                }
                """;

    mockMvc
        .perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(createRequest))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role").value("COFOUNDER"));
  }

  @Test
  @WithMockUser(
      username = "1",
      roles = {"FOUNDER"})
  void shouldCreateFounderProfileUsingDedicatedEndpoint() throws Exception {
    String createRequest =
        """
                {
                  "name": "Maya Founder",
                  "email": "maya@founderlink.com",
                  "skills": "Product",
                  "startupName": "Orbit Labs",
                  "industry": "SaaS",
                  "fundingGoal": 500000
                }
                """;

    mockMvc
        .perform(
            post("/users/founders").contentType(MediaType.APPLICATION_JSON).content(createRequest))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role").value("FOUNDER"))
        .andExpect(jsonPath("$.startupName").value("Orbit Labs"))
        .andExpect(jsonPath("$.industry").value("SaaS"))
        .andExpect(jsonPath("$.fundingGoal").value(500000.0));
  }

  @Test
  @WithMockUser(
      username = "1",
      roles = {"FOUNDER"})
  void shouldRejectFounderCreateWhenRoleSpecificFieldsMissing() throws Exception {
    String invalidRequest =
        """
                {
                  "name": "Maya Founder",
                  "email": "maya@founderlink.com"
                }
                """;

    mockMvc
        .perform(
            post("/users/founders").contentType(MediaType.APPLICATION_JSON).content(invalidRequest))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Validation failed"));
  }
}
