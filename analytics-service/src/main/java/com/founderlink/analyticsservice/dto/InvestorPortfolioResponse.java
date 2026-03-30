package com.founderlink.analyticsservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvestorPortfolioResponse {
  private Long investorId;
  private double totalInvested;
  private long investmentCount;
}
