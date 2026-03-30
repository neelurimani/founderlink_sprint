package com.founderlink.startupservice.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class StartupPostWithHeadersTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void createStartup_withGatewayHeaders_isNotForbidden() throws Exception {
    mockMvc
        .perform(
            post("/startups")
                .header("X-User-Id", "999001")
                .header("X-Roles", "FOUNDER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    String.format(
                        """
                                {"name":"HeaderAuth Co %d","description":"d","industry":"Fintech","fundingGoal":10000}
                                """,
                        System.nanoTime())))
        .andExpect(status().isCreated());
  }
}
