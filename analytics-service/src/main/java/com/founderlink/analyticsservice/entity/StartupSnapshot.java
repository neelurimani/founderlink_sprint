package com.founderlink.analyticsservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "startup_snapshots")
@Getter
@Setter
public class StartupSnapshot {

  @Id private Long startupId;

  private Long founderId;

  private Double fundingGoal;

  private String industry;

  private Instant updatedAt;
}
