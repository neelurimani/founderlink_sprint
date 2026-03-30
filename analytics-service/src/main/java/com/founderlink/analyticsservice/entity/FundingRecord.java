package com.founderlink.analyticsservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "funding_records",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_funding_startup_investor",
          columnNames = {"startup_id", "investor_id"})
    })
@Getter
@Setter
public class FundingRecord {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Idempotent key from investment-service (one row per investment). */
  @Column(unique = true)
  private Long investmentId;

  private Long startupId;
  private Long investorId;
  private Double amount;
  private String status;
  private LocalDate createdDate;

  @PrePersist
  void onCreate() {
    createdDate = LocalDate.now();
  }
}
