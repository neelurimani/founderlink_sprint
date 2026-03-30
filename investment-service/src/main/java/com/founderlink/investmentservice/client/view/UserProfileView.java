package com.founderlink.investmentservice.client.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserProfileView(Long id, String name, String email, String role) {}
