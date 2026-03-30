package com.founderlink.analyticsservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FundingTrendResponse {
  private long recordCount;
  private double totalFunding;
  private double averageFunding;
}
