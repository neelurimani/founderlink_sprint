package com.founderlink.analyticsservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StartupMetricsResponse {
  private Long startupId;

  /** Sum of investment amounts for this startup (one row per investor). */
  private double totalInvested;

  private Double fundingGoal;

  /** 0–100 when fundingGoal is present and positive; otherwise null. */
  private Double fundingProgressPercent;

  /** Founder (if known) + distinct team invitations / members tracked. */
  private long teamSize;

  private long investmentCount;
}
