package com.founderlink.startupservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "startups",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_startup_name_founder",
            columnNames = {"name", "founder_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Startup {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  private String description;

  private String industry;

  private String problemStatement;

  private String solution;

  @Column(nullable = false)
  private Double fundingGoal;

  @Enumerated(EnumType.STRING)
  @Column(length = 32)
  private StartupStage stage;

  @Column(name = "founder_id", nullable = false)
  private Long founderId;

  /** Listing lifecycle: only APPROVED startups are visible in public listings. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private StartupStatus status;

  /**
   * Legacy NOT NULL column on existing deployments. Kept in sync with {@link #status}; drop after
   * DB migration.
   */
  @Column(name = "approved", nullable = false)
  @Builder.Default
  private boolean approved = false;

  private LocalDateTime createdAt;

  @PrePersist
  public void onCreate() {
    this.createdAt = LocalDateTime.now();
    if (this.status == null) {
      this.status = StartupStatus.PENDING;
    }
    syncLegacyApproved();
  }

  @PreUpdate
  public void onUpdate() {
    syncLegacyApproved();
  }

  private void syncLegacyApproved() {
    this.approved = this.status == StartupStatus.APPROVED;
  }
}
