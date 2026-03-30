package com.founderlink.investmentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.Check;

@Entity
@Table(
    name = "investments",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_investment_startup_investor",
            columnNames = {"startup_id", "investor_id"}))
@Check(constraints = "amount > 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Investment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "investor_id", nullable = false)
  private Long investorId;

  @Column(name = "startup_id", nullable = false)
  private Long startupId;

  @Column(nullable = false)
  private Double amount;

  @Column(nullable = false)
  private String status; // PENDING, APPROVED, REJECTED, COMPLETED

  private LocalDateTime createdAt;
}
