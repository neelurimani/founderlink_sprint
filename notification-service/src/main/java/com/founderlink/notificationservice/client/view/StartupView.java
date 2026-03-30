package com.founderlink.notificationservice.client.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StartupView(Long id, Long founderId) {}
