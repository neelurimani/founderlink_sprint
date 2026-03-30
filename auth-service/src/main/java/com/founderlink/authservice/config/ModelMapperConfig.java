package com.founderlink.authservice.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Central mapper for DTO ↔ entity conversions (auth service currently maps in constructors/records;
 * bean is available for future refactors).
 */
@Configuration
public class ModelMapperConfig {

  @Bean
  public ModelMapper modelMapper() {
    ModelMapper mapper = new ModelMapper();
    mapper
        .getConfiguration()
        .setMatchingStrategy(MatchingStrategies.STRICT)
        .setAmbiguityIgnored(true);
    return mapper;
  }
}
