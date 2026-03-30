package com.founderlink.startupservice.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class StartupSecuritySmokeTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void actuatorHealthIsPublic() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void startupsCatalogIsPublic() throws Exception {
    mockMvc.perform(get("/startups")).andExpect(status().isOk());
  }

  @Test
  void startupsByIndustryIsPublic() throws Exception {
    mockMvc.perform(get("/startups/industry/Fintech")).andExpect(status().isOk());
  }
}
