package com.founderlink.authservice.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void registerReturnsPayloadWithoutTokensThenLoginReturnsTokens() throws Exception {
    String email = "test-" + System.currentTimeMillis() + "@founderlink.com";

    String registerBody =
        """
                {
                  "name": "Test Founder",
                  "email": "%s",
                  "password": "Password123!",
                  "role": "FOUNDER"
                }
                """
            .formatted(email);

    mockMvc
        .perform(
            post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value("User registered successfully"))
        .andExpect(jsonPath("$.userId").isNotEmpty())
        .andExpect(jsonPath("$.email").value(email.toLowerCase()));

    String loginBody =
        """
                {
                  "email": "%s",
                  "password": "Password123!"
                }
                """
            .formatted(email);

    mockMvc
        .perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty());
  }
}
