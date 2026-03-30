package com.founderlink.investmentservice;

import com.founderlink.investmentservice.client.StartupServiceClient;
import com.founderlink.investmentservice.client.UserServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("test")
class InvestmentServiceApplicationTests {

  @MockBean private StartupServiceClient startupServiceClient;

  @MockBean private RabbitTemplate rabbitTemplate;

  @MockBean private UserServiceClient userServiceClient;

  @Test
  void contextLoads() {}
}
