package com.founderlink.analyticsservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "startup_team_members",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_startup_team_user",
            columnNames = {"startup_id", "user_id"}))
@Getter
@Setter
public class StartupTeamMember {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "startup_id", nullable = false)
  private Long startupId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  private Instant createdAt;

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }
}
