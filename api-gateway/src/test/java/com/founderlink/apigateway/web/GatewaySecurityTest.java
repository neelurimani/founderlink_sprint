package com.founderlink.apigateway.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewaySecurityTest {

  @LocalServerPort private int port;

  private WebTestClient client() {
    return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void rejectsUnauthorizedRequests() {
    client().get().uri("/api/users/1").exchange().expectStatus().isUnauthorized();
  }

  @Test
  void actuatorHealthIsPublic() {
    client().get().uri("/actuator/health").exchange().expectStatus().isOk();
  }
}
