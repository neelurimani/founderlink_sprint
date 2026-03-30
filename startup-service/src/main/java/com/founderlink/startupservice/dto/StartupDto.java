package com.founderlink.startupservice.dto;

import com.founderlink.startupservice.entity.StartupStage;
import com.founderlink.startupservice.entity.StartupStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartupDto {

  private String name;
  private String description;
  private String industry;
  private String problemStatement;
  private String solution;
  private Double fundingGoal;
  private StartupStage stage;

  /** Only ADMIN may set on update. */
  private StartupStatus status;
}
