package com.founderlink.notificationservice.events;

import lombok.Data;

@Data
public class StartupCreatedEvent {
  private Long startupId;
  private Long founderId;
  private String industry;
  private Double fundingGoal;
}
