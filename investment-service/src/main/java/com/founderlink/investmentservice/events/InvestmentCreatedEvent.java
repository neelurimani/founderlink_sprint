package com.founderlink.investmentservice.events;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvestmentCreatedEvent {
  private Long startupId;
  private Long investorId;
  private Double amount;
}
