package com.founderlink.notificationservice.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(properties = {"spring.cache.type=simple"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class NotificationSecuritySmokeTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void actuatorHealthIsPublic() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void notificationsEndpointDeniedWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/notifications/user/1")).andExpect(status().isForbidden());
  }
}
