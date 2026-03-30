package com.founderlink.notificationservice.events;

import lombok.Data;

@Data
public class InvestmentCreatedEvent {
  private Long startupId;
  private Long investorId;
  private Double amount;
}
