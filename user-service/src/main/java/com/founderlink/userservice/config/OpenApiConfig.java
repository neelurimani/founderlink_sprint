package com.founderlink.userservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI founderLinkOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("FounderLink User Service API")
                .version("v1")
                .description("APIs for user profile management in FounderLink.")
                .contact(new Contact().name("FounderLink Engineering"))
                .license(new License().name("Internal Use")));
  }
}
