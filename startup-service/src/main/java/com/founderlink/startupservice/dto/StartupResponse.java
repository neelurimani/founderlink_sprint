package com.founderlink.startupservice.dto;

import com.founderlink.startupservice.entity.StartupStage;
import com.founderlink.startupservice.entity.StartupStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartupResponse {
  private Long id;
  private String name;
  private String description;
  private String industry;
  private String problemStatement;
  private String solution;
  private Double fundingGoal;
  private StartupStage stage;
  private Long founderId;
  private StartupStatus status;
  private LocalDateTime createdAt;
}
