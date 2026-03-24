package com.founderlink.userservice.config;

import com.founderlink.userservice.dto.UserProfileResponse;
import com.founderlink.userservice.entities.UserProfile;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        mapper.typeMap(UserProfile.class, UserProfileResponse.class)
                .addMappings(cfg -> cfg.map(UserProfile::getPortfolioLink, UserProfileResponse::setPortfolioLinks));
        return mapper;
    }

    @Bean
    public OpenAPI founderLinkOpenApi() {
        return new OpenAPI().info(
                new Info()
                        .title("FounderLink User Service API")
                        .version("v1")
                        .description("APIs for user profile management in FounderLink.")
                        .contact(new Contact().name("FounderLink Engineering"))
                        .license(new License().name("Internal Use"))
        );
    }
}
