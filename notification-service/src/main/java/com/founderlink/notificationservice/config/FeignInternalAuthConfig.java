package com.founderlink.notificationservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignInternalAuthConfig {

  @Bean
  RequestInterceptor internalAuthHeaders() {
    return template -> {
      // Allow notification-service to call other services in local/dev without a user JWT.
      template.header("X-User-Id", "1");
      template.header("X-Roles", "ADMIN");
    };
  }
}
