package com.founderlink.investmentservice.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.founderlink.investmentservice.client.StartupServiceClient;
import com.founderlink.investmentservice.client.UserServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvestmentSecuritySmokeTest {

  @MockBean private StartupServiceClient startupServiceClient;

  @MockBean private RabbitTemplate rabbitTemplate;

  @MockBean private UserServiceClient userServiceClient;

  @Autowired private MockMvc mockMvc;

  @Test
  void actuatorHealthIsPublic() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void investmentsEndpointRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/investments/startup/1")).andExpect(status().isForbidden());
  }
}
