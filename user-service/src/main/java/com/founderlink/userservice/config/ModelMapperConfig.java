package com.founderlink.userservice.config;

import com.founderlink.userservice.dto.UserProfileResponse;
import com.founderlink.userservice.entities.UserProfile;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

  @Bean
  public ModelMapper modelMapper() {
    ModelMapper mapper = new ModelMapper();
    mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
    mapper
        .typeMap(UserProfile.class, UserProfileResponse.class)
        .addMappings(
            cfg -> cfg.map(UserProfile::getPortfolioLink, UserProfileResponse::setPortfolioLinks));
    return mapper;
  }
}
