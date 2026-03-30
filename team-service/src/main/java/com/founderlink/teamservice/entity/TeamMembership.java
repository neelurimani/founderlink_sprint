package com.founderlink.teamservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "team_members",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_team_startup_user",
            columnNames = {"startup_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMembership {

  public enum Status {
    PENDING,
    ACTIVE
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "startup_id", nullable = false)
  private Long startupId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  /** Design-doc examples: CTO, CPO, MARKETING_HEAD, ENGINEERING_LEAD */
  @Column(nullable = false, length = 64)
  private String teamRole;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Status status;

  @Column(name = "invited_by_user_id")
  private Long invitedByUserId;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column private LocalDateTime joinedAt;
}
